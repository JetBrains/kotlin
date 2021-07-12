/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeOperatorAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnmatchedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.idea.fir.getCandidateSymbols
import org.jetbrains.kotlin.idea.fir.isImplicitFunctionCall
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirSafe
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.utils.addIfNotNull

internal object FirReferenceResolveHelper {
    fun FirResolvedTypeRef.toTargetSymbol(session: FirSession, symbolBuilder: KtSymbolByFirBuilder): KtSymbol? {

        val type = getDeclaredType() as? ConeLookupTagBasedType
        val resolvedSymbol = type?.lookupTag?.toSymbol(session) as? FirBasedSymbol<*>

        val symbol = resolvedSymbol ?: run {
            val diagnostic = (this as? FirErrorTypeRef)?.diagnostic
            (diagnostic as? ConeUnmatchedTypeArgumentsError)?.type
        }

        return symbol?.fir?.buildSymbol(symbolBuilder)
    }

    private fun FirResolvedTypeRef.getDeclaredType() =
        if (this.delegatedTypeRef?.source?.kind == FirFakeSourceElementKind.ArrayTypeFromVarargParameter) type.arrayElementType()
        else type

    private fun ClassId.toTargetPsi(
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder,
        calleeReference: FirReference? = null,
    ): KtSymbol? {
        val classLikeDeclaration = ConeClassLikeLookupTagImpl(this).toSymbol(session)?.fir
        if (classLikeDeclaration is FirRegularClass) {
            if (calleeReference is FirResolvedNamedReference) {
                val callee = calleeReference.resolvedSymbol.fir as? FirCallableDeclaration
                // TODO: check callee owner directly?
                if (callee !is FirConstructor && callee?.isStatic != true) {
                    classLikeDeclaration.companionObject?.let { return it.buildSymbol(symbolBuilder) }
                }
            }
        }
        return classLikeDeclaration?.buildSymbol(symbolBuilder)
    }

    fun FirReference.toTargetSymbol(session: FirSession, symbolBuilder: KtSymbolByFirBuilder): Collection<KtSymbol> {
        return when (this) {
            is FirBackingFieldReference -> {
                listOfNotNull(symbolBuilder.variableLikeBuilder.buildBackingFieldSymbol(resolvedSymbol))
            }
            is FirResolvedNamedReference -> {
                val fir = when (val symbol = resolvedSymbol) {
                    is FirSyntheticPropertySymbol -> {
                        val syntheticProperty = symbol.fir as FirSyntheticProperty
                        if (syntheticProperty.getter.delegate.symbol.callableId == symbol.accessorId) {
                            syntheticProperty.getter.delegate
                        } else {
                            syntheticProperty.setter!!.delegate
                        }
                    }
                    else -> symbol.fir
                }
                listOfNotNull(fir.buildSymbol(symbolBuilder))
            }
            is FirResolvedCallableReference -> {
                listOfNotNull(resolvedSymbol.fir.buildSymbol(symbolBuilder))
            }
            is FirThisReference -> {
                listOfNotNull(boundSymbol?.fir?.buildSymbol(symbolBuilder))
            }
            is FirSuperReference -> {
                listOfNotNull((superTypeRef as? FirResolvedTypeRef)?.toTargetSymbol(session, symbolBuilder))
            }
            is FirErrorNamedReference -> {
                getCandidateSymbols().map { it.fir.buildSymbol(symbolBuilder) }
            }
            else -> emptyList()
        }
    }

    private fun getPackageSymbolFor(
        expression: KtSimpleNameExpression,
        symbolBuilder: KtSymbolByFirBuilder,
        forQualifiedType: Boolean
    ): KtFirPackageSymbol? {
        return symbolBuilder.createPackageSymbolIfOneExists(getQualifierSelected(expression, forQualifiedType))
    }

    private fun getQualifierSelected(
        expression: KtSimpleNameExpression,
        forQualifiedType: Boolean
    ): FqName {
        val qualified = when {
            forQualifiedType -> expression.parent?.takeIf { it is KtUserType && it.referenceExpression === expression }
            else -> expression.getQualifiedExpressionForSelector()
        }
        return when (qualified) {
            null -> FqName(expression.getReferencedName())
            else -> {
                qualified
                    .collectDescendantsOfType<KtSimpleNameExpression>()
                    .dropWhile { it.getReferencedName() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE }
                    .joinToString(separator = ".") { it.getReferencedName() }
                    .let(::FqName)
            }
        }
    }

    private fun KtSimpleNameExpression.isPartOfQualifiedExpression(): Boolean {
        var parent = parent
        while (parent is KtDotQualifiedExpression) {
            if (parent.selectorExpression !== this) return true
            parent = parent.parent
        }
        return false
    }

    private fun KtSimpleNameExpression.isPartOfUserTypeRefQualifier(): Boolean {
        var parent = parent
        while (parent is KtUserType) {
            if (parent.referenceExpression !== this) return true
            parent = parent.parent
        }
        return false
    }


    internal fun resolveSimpleNameReference(
        ref: KtFirSimpleNameReference,
        analysisSession: KtFirAnalysisSession
    ): Collection<KtSymbol> {
        val expression = ref.expression
        if (expression.isSyntheticOperatorReference()) return emptyList()
        val symbolBuilder = analysisSession.firSymbolBuilder
        val fir = expression.getOrBuildFir(analysisSession.firResolveState)
        val session = analysisSession.firResolveState.rootModuleSession
        return when (fir) {
            is FirResolvedTypeRef -> getSymbolsForResolvedTypeRef(fir, expression, session, symbolBuilder)
            is FirResolvedQualifier ->
                getSymbolsForResolvedQualifier(fir, expression, session, symbolBuilder)
            is FirAnnotationCall -> getSymbolsForAnnotationCall(fir, session, symbolBuilder)
            is FirResolvedImport -> getSymbolsByResolvedImport(expression, symbolBuilder, fir, session)
            is FirPackageDirective -> getSymbolsForPackageDirective(expression, symbolBuilder)
            is FirFile -> getSymbolsByFirFile(symbolBuilder, fir)
            is FirArrayOfCall -> {
                // We can't yet find PsiElement for arrayOf, intArrayOf, etc.
                emptyList()
            }
            is FirReturnExpression -> getSymbolsByReturnExpression(expression, fir, symbolBuilder)
            is FirErrorNamedReference -> getSymbolsByErrorNamedReference(fir, symbolBuilder)
            is FirVariableAssignment -> getSymbolsByVariableAssignment(fir, session, symbolBuilder)
            is FirResolvedNamedReference -> getSymbolByResolvedNameReference(fir, session, symbolBuilder)
            is FirResolvable -> getSymbolsByResolvable(fir, expression, session, symbolBuilder)
            is FirNamedArgumentExpression -> getSymbolsByNameArgumentExpression(expression, analysisSession, symbolBuilder)
            else -> handleUnknownFirElement(expression, analysisSession, session, symbolBuilder)
        }
    }

    private fun getSymbolsForPackageDirective(
        expression: KtSimpleNameExpression,
        symbolBuilder: KtSymbolByFirBuilder
    ): List<KtFirPackageSymbol> {
        return listOfNotNull(getPackageSymbolFor(expression, symbolBuilder, forQualifiedType = false))
    }


    private fun getSymbolByResolvedNameReference(
        fir: FirResolvedNamedReference,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> = fir.toTargetSymbol(session, symbolBuilder)

    private fun KtSimpleNameExpression.isSyntheticOperatorReference() = when (this) {
        is KtOperationReferenceExpression -> operationSignTokenType in syntheticTokenTypes
        else -> false
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getSymbolsByVariableAssignment(
        fir: FirVariableAssignment,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> = fir.calleeReference.toTargetSymbol(session, symbolBuilder)

    private fun getSymbolsByNameArgumentExpression(
        expression: KtSimpleNameExpression,
        analysisSession: KtFirAnalysisSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        val ktValueArgumentName = expression.parent as? KtValueArgumentName ?: return emptyList()
        val ktValueArgument = ktValueArgumentName.parent as? KtValueArgument ?: return emptyList()
        val ktValueArgumentList = ktValueArgument.parent as? KtValueArgumentList ?: return emptyList()
        val ktCallExpression = ktValueArgumentList.parent as? KtCallElement ?: return emptyList()

        val firCall = ktCallExpression.getOrBuildFirSafe<FirCall>(analysisSession.firResolveState) ?: return emptyList()
        val parameter = firCall.findCorrespondingParameter(ktValueArgument) ?: return emptyList()
        return listOfNotNull(parameter.buildSymbol(symbolBuilder))
    }

    private fun FirCall.findCorrespondingParameter(ktValueArgument: KtValueArgument): FirValueParameter? =
        argumentMapping?.entries?.firstNotNullOfOrNull { (firArgument, firParameter) ->
            if (firArgument.psi == ktValueArgument) firParameter
            else null
        }

    private fun handleUnknownFirElement(
        expression: KtSimpleNameExpression,
        analysisSession: KtFirAnalysisSession,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): List<KtSymbol> {
        // Handle situation when we're in the middle/beginning of qualifier
        // <caret>A.B.C.foo() or A.<caret>B.C.foo()
        // NB: in this case we get some parent FIR, like FirBlock, FirProperty, FirFunction or the like
        var parent = expression.parent as? KtDotQualifiedExpression
        var unresolvedCounter = 1
        while (parent != null) {
            val selectorExpression = parent.selectorExpression ?: break
            if (selectorExpression === expression) {
                parent = parent.parent as? KtDotQualifiedExpression
                continue
            }
            val parentFir = selectorExpression.getOrBuildFir(analysisSession.firResolveState)
            if (parentFir is FirResolvedQualifier) {
                var classId = parentFir.classId
                while (unresolvedCounter > 0) {
                    unresolvedCounter--
                    classId = classId?.outerClassId
                }
                return listOfNotNull(classId?.toTargetPsi(session, symbolBuilder))
            }
            parent = parent.parent as? KtDotQualifiedExpression
            unresolvedCounter++
        }
        return emptyList()
    }

    private fun getSymbolsByResolvable(
        fir: FirResolvable,
        expression: KtSimpleNameExpression,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        val calleeReference =
            if (fir is FirFunctionCall
                && fir.isImplicitFunctionCall()
                && expression is KtNameReferenceExpression
            ) {
                // we are resolving implicit invoke call, like
                // fun foo(a: () -> Unit) {
                //     <expression>a</expression>()
                // }
                (fir.dispatchReceiver as FirQualifiedAccessExpression).calleeReference
            } else fir.calleeReference
        return calleeReference.toTargetSymbol(session, symbolBuilder)
    }


    private fun getSymbolsByErrorNamedReference(
        fir: FirErrorNamedReference,
        symbolBuilder: KtSymbolByFirBuilder
    ): List<KtSymbol> =
        getFirSymbolsByErrorNamedReference(fir).map { it.fir.buildSymbol(symbolBuilder) }


    fun getFirSymbolsByErrorNamedReference(
        errorNamedReference: FirErrorNamedReference,
    ): Collection<FirBasedSymbol<*>> = when (val diagnostic = errorNamedReference.diagnostic) {
        is ConeAmbiguityError -> diagnostic.candidates.map { it.symbol }
        is ConeOperatorAmbiguityError -> diagnostic.candidates
        is ConeInapplicableCandidateError -> listOf(diagnostic.candidate.symbol)
        else -> emptyList()
    }


    private fun getSymbolsByReturnExpression(
        expression: KtSimpleNameExpression,
        fir: FirReturnExpression,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        return if (expression is KtLabelReferenceExpression) {
            listOf(fir.target.labeledElement.buildSymbol(symbolBuilder))
        } else emptyList()
    }

    private fun getSymbolsByFirFile(
        symbolBuilder: KtSymbolByFirBuilder,
        fir: FirFile
    ): List<KtSymbol> {
        return listOf(symbolBuilder.buildSymbol(fir))
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getSymbolsByResolvedImport(
        expression: KtSimpleNameExpression,
        builder: KtSymbolByFirBuilder,
        fir: FirResolvedImport,
        session: FirSession
    ): List<KtSymbol> {
        val fullFqName = fir.importedFqName
        val selectedFqName = getQualifierSelected(expression, forQualifiedType = false)
        val rawImportForSelectedFqName = buildImport {
            importedFqName = selectedFqName
            isAllUnder = false
        }
        val resolvedImport = FirImportResolveTransformer(session).transformImport(rawImportForSelectedFqName, null) as FirResolvedImport
        val scope = FirExplicitSimpleImportingScope(listOf(resolvedImport), session, ScopeSession())
        val selectedName = resolvedImport.importedName ?: return emptyList()
        return buildList {
            if (selectedFqName == fullFqName) {
                // callables cannot be used as receiver expressions in imports
                scope.processFunctionsByName(selectedName) { add(it.fir.buildSymbol(builder)) }
                scope.processPropertiesByName(selectedName) { add(it.fir.buildSymbol(builder)) }
            }
            scope.processClassifiersByName(selectedName) { addIfNotNull(it.fir.buildSymbol(builder)) }
            builder.createPackageSymbolIfOneExists(selectedFqName)?.let(::add)
        }
    }

    private fun getSymbolsForResolvedTypeRef(
        fir: FirResolvedTypeRef,
        expression: KtSimpleNameExpression,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder,
    ): Collection<KtSymbol> {

        val isPossiblyPackage = fir is FirErrorTypeRef && expression.isPartOfUserTypeRefQualifier()

        val resultSymbol =
            if (isPossiblyPackage) getPackageSymbolFor(expression, symbolBuilder, forQualifiedType = true)
            else fir.toTargetSymbol(session, symbolBuilder)

        return listOfNotNull(resultSymbol)
    }

    private fun getSymbolsForResolvedQualifier(
        fir: FirResolvedQualifier,
        expression: KtSimpleNameExpression,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        val referencedSymbol = if (fir.resolvedToCompanionObject) {
            (fir.symbol?.fir as? FirRegularClass)?.companionObject?.symbol
        } else {
            fir.symbol
        }
        if (referencedSymbol == null) {
            // If referencedSymbol is null, it means the reference goes to a package.
            val parent = expression.parent as? KtDotQualifiedExpression ?: return emptyList()
            val fqNameSegments =
                when (expression) {
                    parent.selectorExpression -> parent.fqNameSegments() ?: return emptyList()
                    parent.receiverExpression -> listOf(expression.getReferencedName())
                    else -> return emptyList()
                }
            return listOfNotNull(symbolBuilder.createPackageSymbolIfOneExists(FqName.fromSegments(fqNameSegments)))
        }
        val referencedClass = referencedSymbol.fir
        val referencedSymbolsByFir = listOfNotNull(symbolBuilder.buildSymbol(referencedClass))
        val firSourcePsi = fir.source.psi ?: referencedSymbolsByFir
        // The source of an `FirResolvedQualifier` is either a KtNamedReferenceExpression or a KtDotQualifiedExpression. In the former case,
        // it implies the qualifier is an atomic reference and therefore, it should be identical with the `expression`. In the latter case,
        // we need to manually break up the qualified access and resolve individual parts of it because in FIR, the entire qualified access
        // is one element.
        if (firSourcePsi === expression) return referencedSymbolsByFir
        require(firSourcePsi is KtDotQualifiedExpression)

        if (referencedClass.isLocal) {
            // TODO: handle local classes after KT-47135 is fixed
            return referencedSymbolsByFir
        } else {
            var qualifiedAccess: KtDotQualifiedExpression = firSourcePsi
            val referencedClassId =
                if ((referencedClass as? FirRegularClass)?.isCompanion == true &&
                    (qualifiedAccess.selectorExpression as? KtNameReferenceExpression)?.getReferencedName() != "Companion"
                ) {
                    // Remove the last "Companion" part if the qualified access does not contain it. This is needed because the "Companion"
                    // part is optional.
                    referencedClass.classId.outerClassId ?: return referencedSymbolsByFir
                } else {
                    referencedClass.classId
                }
            val qualifiedAccessSegments = qualifiedAccess.fqNameSegments() ?: return referencedSymbolsByFir
            assert(referencedClassId.asSingleFqName().pathSegments().takeLast(qualifiedAccessSegments.size)
                       .map { it.identifierOrNullIfSpecial } == qualifiedAccessSegments) {
                "Referenced classId $referencedClassId should end with qualifiedAccess expression ${qualifiedAccess.text} "
            }

            // In the code below, we always maintain the contract that `classId` and `qualifiedAccess` should stay "in-sync", i.e. they
            // refer to the same class and classId should be null if `qualifiedAccess` references to a package.
            var classId: ClassId? = referencedClassId

            // Handle nested classes.
            while (classId != null) {
                if (expression === qualifiedAccess.selectorExpression) {
                    return listOfNotNull(classId.toTargetPsi(session, symbolBuilder))
                }
                val outerClassId = classId.outerClassId
                val receiverExpression = qualifiedAccess.receiverExpression
                if (receiverExpression !is KtDotQualifiedExpression) {
                    // If the receiver is not a KtDotQualifiedExpression, it means we are hitting the end of nested receivers. In other
                    // words, this receiver expression should be pointing at an unqualified name of a class, whose class ID is
                    // `outerClassId`.
                    if (receiverExpression == expression) {
                        // If there is still an outer class, then return symbol of that class
                        outerClassId?.let { return listOfNotNull(it.toTargetPsi(session, symbolBuilder)) }
                        // Otherwise, it should be a package, so we return that
                        return listOfNotNull(symbolBuilder.createPackageSymbolIfOneExists(classId.packageFqName))
                    } else {
                        // This is unexpected. The code probably contains some weird structures. In this case, we just fail the resolution
                        // with zero results.
                        return emptyList()
                    }
                }
                qualifiedAccess = receiverExpression
                classId = outerClassId
            }

            // Handle package names
            var packageFqName = referencedClassId.packageFqName

            while (!packageFqName.isRoot) {
                if (expression === qualifiedAccess.selectorExpression) {
                    return listOfNotNull(symbolBuilder.createPackageSymbolIfOneExists(packageFqName))
                }
                val parentPackageFqName = packageFqName.parent()
                val receiverExpression = qualifiedAccess.receiverExpression
                if (receiverExpression !is KtDotQualifiedExpression) {
                    // If the receiver is not a KtDotQualifiedExpression, it means we are hitting the end of nested receivers. In other
                    // words, this receiver expression should be pointing at a top-level package now.
                    if (receiverExpression == expression) {
                        return listOfNotNull(symbolBuilder.createPackageSymbolIfOneExists(parentPackageFqName))
                    } else {
                        // This is unexpected. The code probably contains some weird structures. In this case, we just fail the resolution
                        // with zero results.
                        return emptyList()
                    }
                }
                qualifiedAccess = receiverExpression
                packageFqName = parentPackageFqName
            }
            return referencedSymbolsByFir
        }
    }

    /**
     * Returns the segments of a qualified access PSI. For example, given `foo.bar.OuterClass.InnerClass`, this returns `["foo", "bar",
     * "OuterClass", "InnerClass"]`.
     */
    private fun KtDotQualifiedExpression.fqNameSegments(): List<String>? {
        val result: MutableList<String> = mutableListOf()
        var current: KtExpression = this
        while (current is KtDotQualifiedExpression) {
            result += (current.selectorExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return null
            current = current.receiverExpression
        }
        result += (current as? KtNameReferenceExpression)?.getReferencedName() ?: return null
        result.reverse()
        return result
    }

    private fun getSymbolsForAnnotationCall(
        fir: FirAnnotationCall,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        val type = fir.typeRef as? FirResolvedTypeRef ?: return emptyList()
        return listOfNotNull(type.toTargetSymbol(session, symbolBuilder))
    }

    private fun findPossibleTypeQualifier(
        qualifier: KtSimpleNameExpression,
        wholeTypeFir: FirResolvedTypeRef
    ): ClassId? {
        val qualifierToResolve = qualifier.parent as KtUserType
        // FIXME make it work with generics in functional types (like () -> AA.BB<CC, AA.DD>)
        val wholeType = when (val psi = wholeTypeFir.psi) {
            is KtUserType -> psi
            is KtTypeReference -> psi.typeElement?.unwrapNullability() as? KtUserType
            else -> null
        } ?: return null

        val qualifiersToDrop = countQualifiersToDrop(wholeType, qualifierToResolve)
        return wholeTypeFir.type.classId?.dropLastNestedClasses(qualifiersToDrop)
    }

    /**
     * @return class id without [classesToDrop] last nested classes, or `null` if [classesToDrop] is too big.
     *
     * Example: `foo.bar.Baz.Inner` with 1 dropped class is `foo.bar.Baz`, and with 2 dropped class is `null`.
     */
    private fun ClassId.dropLastNestedClasses(classesToDrop: Int) =
        generateSequence(this) { it.outerClassId }.drop(classesToDrop).firstOrNull()

    /**
     * @return How many qualifiers needs to be dropped from [wholeType] to get [nestedType].
     *
     * Example: to get `foo.bar` from `foo.bar.Baz.Inner`, you need to drop 2 qualifiers (`Inner` and `Baz`).
     */
    private fun countQualifiersToDrop(wholeType: KtUserType, nestedType: KtUserType): Int {
        val qualifierIndex = generateSequence(wholeType) { it.qualifier }.indexOf(nestedType)
        require(qualifierIndex != -1) { "Whole type $wholeType should contain $nestedType, but it didn't" }
        return qualifierIndex
    }

    private val syntheticTokenTypes = TokenSet.create(KtTokens.ELVIS, KtTokens.EXCLEXCL)
}

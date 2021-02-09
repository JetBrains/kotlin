/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.calls.SyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeOperatorAmbiguityError
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.idea.fir.*
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
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal object FirReferenceResolveHelper {
    fun FirResolvedTypeRef.toTargetSymbol(session: FirSession, symbolBuilder: KtSymbolByFirBuilder): KtSymbol? {
        val type = type as? ConeLookupTagBasedType ?: return null
        val symbol = type.lookupTag.toSymbol(session) as? AbstractFirBasedSymbol<*>
        return symbol?.fir?.buildSymbol(symbolBuilder)
    }

    private fun ClassId.toTargetPsi(
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder,
        calleeReference: FirReference? = null
    ): KtSymbol? {
        val classLikeDeclaration = ConeClassLikeLookupTagImpl(this).toSymbol(session)?.fir
        if (classLikeDeclaration is FirRegularClass) {
            if (calleeReference is FirResolvedNamedReference) {
                val callee = calleeReference.resolvedSymbol.fir as? FirCallableMemberDeclaration
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
            is FirResolvedNamedReference -> {
                val fir = when (val symbol = resolvedSymbol) {
                    is SyntheticPropertySymbol -> {
                        val syntheticProperty = symbol.fir as FirSyntheticProperty
                        if (syntheticProperty.getter.delegate.symbol.callableId == symbol.accessorId) {
                            syntheticProperty.getter.delegate
                        } else {
                            syntheticProperty.setter!!.delegate
                        }
                    }
                    else -> symbol.fir as? FirDeclaration
                }
                listOfNotNull(fir?.buildSymbol(symbolBuilder))
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
                getCandidateSymbols().mapNotNull { it.fir.buildSymbol(symbolBuilder) }
            }
            else -> emptyList()
        }
    }

    private fun getPackageSymbolFor(
        expression: KtSimpleNameExpression,
        symbolBuilder: KtSymbolByFirBuilder,
        forQualifiedType: Boolean
    ): KtFirPackageSymbol? {
        val qualified = when {
            forQualifiedType -> expression.parent?.takeIf { it is KtUserType && it.referenceExpression === expression }
            else -> expression.getQualifiedExpressionForSelector()
        }
        val fqName = when (qualified) {
            null -> FqName(expression.getReferencedName())
            else -> {
                qualified
                    .collectDescendantsOfType<KtSimpleNameExpression>()
                    .joinToString(separator = ".") { it.getReferencedName() }
                    .let(::FqName)
            }
        }
        return symbolBuilder.createPackageSymbolIfOneExists(fqName)
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
                getSymbolsForResolvedQualifier(fir, expression, session, symbolBuilder, analysisSession)
            is FirAnnotationCall -> getSymbolsForAnnotationCall(fir, session, symbolBuilder)
            is FirResolvedImport -> getSymbolsByResolvedImport(expression, symbolBuilder, fir, session)
            is FirFile -> getSymbolsByFirFile(expression, symbolBuilder, fir)
            is FirArrayOfCall -> {
                // We can't yet find PsiElement for arrayOf, intArrayOf, etc.
                emptyList()
            }
            is FirReturnExpression -> getSymbolsByReturnExpression(expression, fir, symbolBuilder)
            is FirErrorNamedReference -> getSymbolsByErrorNamedReference(fir, symbolBuilder)
            is FirVariableAssignment -> getSymbolsByVariableAssignment(fir, session, symbolBuilder)
            is FirResolvable -> getSymbolsByResolvable(fir, expression, session, symbolBuilder)
            is FirNamedArgumentExpression -> getSymbolsByNameArgumentExpression(expression, analysisSession, symbolBuilder)
            else -> handleUnknownFirElement(expression, analysisSession, session, symbolBuilder)
        }
    }

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
        argumentMapping?.entries?.firstNotNullResult { (firArgument, firParameter) ->
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
    ): List<KtSymbol> {
        val candidates = when (val diagnostic = fir.diagnostic) {
            is ConeAmbiguityError -> diagnostic.candidates
            is ConeOperatorAmbiguityError -> diagnostic.candidates
            is ConeInapplicableCandidateError -> listOf(diagnostic.candidateSymbol)
            else -> emptyList()
        }
        return candidates.mapNotNull { it.fir.buildSymbol(symbolBuilder) }
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
        expression: KtSimpleNameExpression,
        symbolBuilder: KtSymbolByFirBuilder,
        fir: FirFile
    ): List<KtSymbol> {
        if (expression.getNonStrictParentOfType<KtPackageDirective>() != null) {
            // Special: package reference in the middle of package directive
            return listOfNotNull(getPackageSymbolFor(expression, symbolBuilder, forQualifiedType = false))
        }
        return listOf(symbolBuilder.buildSymbol(fir))
    }

    private fun getSymbolsByResolvedImport(
        expression: KtSimpleNameExpression,
        symbolBuilder: KtSymbolByFirBuilder,
        fir: FirResolvedImport,
        session: FirSession
    ): List<KtSymbol> {
        if (expression.isPartOfQualifiedExpression()) {
            return listOfNotNull(getPackageSymbolFor(expression, symbolBuilder, forQualifiedType = false))
        }

        val classId = fir.resolvedClassId
        if (classId != null) {
            return listOfNotNull(classId.toTargetPsi(session, symbolBuilder))
        }
        val name = fir.importedName ?: return emptyList()
        val symbolProvider = session.firSymbolProvider

        @OptIn(ExperimentalStdlibApi::class)
        return buildList {
            symbolProvider.getTopLevelCallableSymbols(fir.packageFqName, name)
                .mapTo(this) { it.fir.buildSymbol(symbolBuilder) }
            symbolProvider
                .getClassLikeSymbolByFqName(ClassId(fir.packageFqName, name))
                ?.fir
                ?.buildSymbol(symbolBuilder)
                ?.let(::add)
        }
    }

    private fun getSymbolsForResolvedTypeRef(
        fir: FirResolvedTypeRef,
        expression: KtSimpleNameExpression,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        if (expression.isPartOfUserTypeRefQualifier()) {
            val typeQualifier = findPossibleTypeQualifier(expression, fir)?.toTargetPsi(session, symbolBuilder)
            val typeOrPackageQualifier =
                typeQualifier ?: getPackageSymbolFor(expression, symbolBuilder, forQualifiedType = true)

            return listOfNotNull(typeOrPackageQualifier)
        }
        return listOfNotNull(fir.toTargetSymbol(session, symbolBuilder))
    }

    private fun getSymbolsForResolvedQualifier(
        fir: FirResolvedQualifier,
        expression: KtSimpleNameExpression,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder,
        analysisSession: KtFirAnalysisSession
    ): Collection<KtSymbol> {
        // TODO refactor that block
        val classId = fir.classId ?: return emptyList()

        var parent = expression.parent as? KtDotQualifiedExpression
        // Distinguish A.foo() from A(.Companion).foo()
        // Make expression.parent as? KtDotQualifiedExpression local function
        while (parent != null) {
            val selectorExpression = parent.selectorExpression ?: break
            if (selectorExpression === expression) {
                parent = parent.parent as? KtDotQualifiedExpression
                continue
            }
            val receiverClassId = if (parent.receiverExpression == expression) {
                /*
                 * <caret>A.Named.i -> class A
                 */
                val name = fir.relativeClassFqName?.pathSegments()?.firstOrNull()
                name?.let { ClassId(fir.packageFqName, it) }
            } else null
            val parentFir = selectorExpression.getOrBuildFir(analysisSession.firResolveState)
            when {
                parentFir is FirQualifiedAccess -> {
                    return listOfNotNull(
                        (receiverClassId ?: classId).toTargetPsi(session, symbolBuilder, parentFir.calleeReference)
                    )
                }
                receiverClassId != null -> {
                    return listOfNotNull(receiverClassId.toTargetPsi(session, symbolBuilder))
                }
                else -> parent = parent.parent as? KtDotQualifiedExpression
            }
        }
        return listOfNotNull(classId.toTargetPsi(session, symbolBuilder))
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
            is KtTypeReference -> psi.typeElement?.unwrapNullable() as? KtUserType
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

    private tailrec fun KtTypeElement.unwrapNullable(): KtTypeElement? {
        return if (this is KtNullableType) innerType?.unwrapNullable() else this
    }

    private val syntheticTokenTypes = TokenSet.create(KtTokens.ELVIS, KtTokens.EXCLEXCL)
}

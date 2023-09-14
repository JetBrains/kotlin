/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.throwUnexpectedFirElementError
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnmatchedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

internal object FirReferenceResolveHelper {
    fun FirResolvedTypeRef.toTargetSymbol(session: FirSession, symbolBuilder: KtSymbolByFirBuilder): KtSymbol? {
        val type = getDeclaredType() as? ConeLookupTagBasedType

        return type?.toTargetSymbol(session, symbolBuilder) ?: run {
            val diagnostic = (this as? FirErrorTypeRef)?.diagnostic
            (diagnostic as? ConeUnmatchedTypeArgumentsError)?.symbol?.fir?.buildSymbol(symbolBuilder)
        }
    }

    private fun ConeKotlinType.toTargetSymbol(session: FirSession, symbolBuilder: KtSymbolByFirBuilder): KtSymbol? {
        val type = this as? ConeLookupTagBasedType
        val resolvedSymbol = type?.lookupTag?.toSymbol(session) as? FirBasedSymbol<*>

        val symbol = resolvedSymbol ?: run {
            val diagnostic = (this as? ConeErrorType)?.diagnostic
            (diagnostic as? ConeUnmatchedTypeArgumentsError)?.symbol
        }

        return symbol?.fir?.buildSymbol(symbolBuilder)
    }

    private fun FirResolvedTypeRef.getDeclaredType() =
        if (this.delegatedTypeRef?.source?.kind == KtFakeSourceElementKind.ArrayTypeFromVarargParameter) type.arrayElementType()
        else type

    private fun ClassId.toTargetPsi(
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder,
        calleeReference: FirReference? = null,
    ): KtSymbol? {
        val classLikeDeclaration = this.toSymbol(session)?.fir
        if (classLikeDeclaration is FirRegularClass) {
            if (calleeReference is FirResolvedNamedReference) {
                val callee = calleeReference.resolvedSymbol.fir as? FirCallableDeclaration
                // TODO: check callee owner directly?
                if (callee !is FirConstructor && callee?.isStatic != true) {
                    classLikeDeclaration.companionObjectSymbol?.let { return it.fir.buildSymbol(symbolBuilder) }
                }
            }
        }
        return classLikeDeclaration?.buildSymbol(symbolBuilder)
    }

    fun FirReference.toTargetSymbol(
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder,
        isInLabelReference: Boolean = false
    ): Collection<KtSymbol> {
        return when (this) {
            is FirBackingFieldReference -> {
                listOfNotNull(resolvedSymbol.fir.buildSymbol(symbolBuilder))
            }
            is FirResolvedCallableReference -> {
                listOfNotNull(resolvedSymbol.fir.buildSymbol(symbolBuilder))
            }
            is FirResolvedNamedReference -> {
                listOfNotNull(resolvedSymbol.buildSymbol(symbolBuilder))
            }
            is FirThisReference -> {
                val boundSymbol = boundSymbol
                when {
                    !isInLabelReference && boundSymbol is FirCallableSymbol<*> ->
                        symbolBuilder.callableBuilder.buildExtensionReceiverSymbol(boundSymbol)
                    else -> boundSymbol?.fir?.buildSymbol(symbolBuilder)
                }.let { listOfNotNull(it) }
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
                val refs =
                    if (qualified is KtUserType && qualified.stub != null && (qualified.containingFile as? KtFile)?.isCompiled == true) {
                        collectTypeReferences(qualified)
                    } else {
                        qualified.collectDescendantsOfType<KtSimpleNameExpression>()
                    }
                refs.map { it.getReferencedName() }
                    .dropWhile { it == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE }
                    .joinToString(separator = ".")
                    .let(::FqName)
            }
        }
    }

    private fun collectTypeReferences(qualified: KtUserType): MutableList<KtNameReferenceExpression> {
        val refs = mutableListOf<KtNameReferenceExpression>()
        fun collectFragments(type: KtUserType) {
            type.getStubOrPsiChild(KtStubElementTypes.USER_TYPE)?.let { collectFragments(it) }
            refs.add(type.referenceExpression as? KtNameReferenceExpression ?: return)
        }
        collectFragments(qualified)
        return refs
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

    internal fun adjustResolutionExpression(expression: KtElement): KtElement {
        // If we are at a super-type constructor call, adjust the resolution expression so that we
        // get the constructor instead of the class.
        //
        // For the example:
        //
        // class A {
        //   constructor()
        // }
        // class B: <caret>A()
        //
        // We want to resolve to the secondary constructor in A. Therefore, we check that the caret is at a supertype
        // call entry and if so we resolve the constructor callee expression.
        val userType = expression.parent as? KtUserType ?: return expression
        val typeReference = userType.parent as? KtTypeReference ?: return expression
        val constructorCalleeExpression = typeReference.parent as? KtConstructorCalleeExpression ?: return expression
        return if (constructorCalleeExpression.parent is KtSuperTypeCallEntry) constructorCalleeExpression else expression
    }


    internal fun resolveSimpleNameReference(
        ref: KtFirSimpleNameReference,
        analysisSession: KtFirAnalysisSession
    ): Collection<KtSymbol> {
        val expression = ref.expression
        if (expression.isSyntheticOperatorReference()) return emptyList()
        val symbolBuilder = analysisSession.firSymbolBuilder
        val adjustedResolutionExpression = adjustResolutionExpression(expression)
        val fir = when (val baseFir = adjustedResolutionExpression.getOrBuildFir(analysisSession.firResolveSession)) {
            is FirSmartCastExpression -> baseFir.originalExpression
            else -> baseFir
        }
        val session = analysisSession.firResolveSession.useSiteFirSession
        return when (fir) {
            is FirResolvedTypeRef -> getSymbolsForResolvedTypeRef(fir, expression, session, symbolBuilder)
            is FirResolvedQualifier ->
                getSymbolsForResolvedQualifier(fir, expression, session, symbolBuilder)
            is FirAnnotation -> getSymbolsForAnnotationCall(fir, session, symbolBuilder)
            is FirResolvedImport -> getSymbolsByResolvedImport(expression, symbolBuilder, fir, session)
            is FirPackageDirective -> getSymbolsForPackageDirective(expression, symbolBuilder)
            is FirFile -> getSymbolsByFirFile(symbolBuilder, fir)
            is FirArrayLiteral -> {
                // We can't yet find PsiElement for arrayOf, intArrayOf, etc.
                emptyList()
            }
            is FirReturnExpression -> getSymbolsByReturnExpression(expression, fir, symbolBuilder)
            is FirErrorNamedReference -> getSymbolsByErrorNamedReference(fir, symbolBuilder)
            is FirVariableAssignment -> getSymbolsByVariableAssignment(fir, expression, session, symbolBuilder)
            is FirResolvedNamedReference -> getSymbolByResolvedNameReference(fir, expression, analysisSession, session, symbolBuilder)
            is FirDelegatedConstructorCall ->
                getSymbolByDelegatedConstructorCall(expression, adjustedResolutionExpression, fir, session, symbolBuilder)
            is FirResolvable -> getSymbolsByResolvable(fir, expression, session, symbolBuilder)
            is FirNamedArgumentExpression -> getSymbolsByNameArgumentExpression(expression, analysisSession, symbolBuilder)
            is FirEqualityOperatorCall -> getSymbolsByEqualsName(fir, session, analysisSession, symbolBuilder)
            is FirTypeParameter -> getSybmolsByTypeParameter(symbolBuilder, fir)
            is FirResolvedReifiedParameterReference -> getSymbolsByResolvedReifiedTypeParameterReference(symbolBuilder, fir)
            else -> handleUnknownFirElement(expression, analysisSession, session, symbolBuilder)
        }
    }

    private fun getSymbolsByEqualsName(
        expression: FirEqualityOperatorCall,
        session: FirSession,
        analysisSession: KtFirAnalysisSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        val lhs = expression.arguments.firstOrNull() ?: return emptyList()
        val scope = lhs.resolvedType.scope(
            session,
            analysisSession.getScopeSessionFor(analysisSession.useSiteSession),
            CallableCopyTypeCalculator.DoNothing,
            requiredMembersPhase = FirResolvePhase.STATUS,
        ) ?: return emptyList()

        return buildList {
            scope.processFunctionsByName(OperatorNameConventions.EQUALS) { functionSymbol ->
                val parameterSymbol = functionSymbol.valueParameterSymbols.singleOrNull()
                if (parameterSymbol != null && parameterSymbol.fir.returnTypeRef.coneType.fullyExpandedType(session).isNullableAny) {
                    add(functionSymbol.buildSymbol(symbolBuilder))
                }
            }
        }
    }

    private fun getSymbolByDelegatedConstructorCall(
        expression: KtSimpleNameExpression,
        adjustedResolutionExpression: KtElement,
        fir: FirDelegatedConstructorCall,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        if (expression != adjustedResolutionExpression) {
            // Type alias detection.
            //
            // If we adjusted resolution to get a constructor instead of a class, we need to undo that
            // if the class is defined as a type alias. We can detect that situation when the constructed type
            // is different from the return type of the constructor.
            //
            // TODO: This seems a little indirect. Is there a better way to do this? For FE1.0 there is
            // a special `TypeAliasConstructorDescriptor` for this case. For FIR there is
            // FirConstructor.originalConstructorIfTypeAlias but that doesn't seem to help here as it
            // is null for the constructors we get.
            val constructedType = fir.constructedTypeRef.coneType
            val constructorReturnType = fir.calleeReference.toResolvedConstructorSymbol()?.resolvedReturnTypeRef?.type
            if (constructedType.classId != constructorReturnType?.classId) {
                return getSymbolsForResolvedTypeRef(fir.constructedTypeRef as FirResolvedTypeRef, expression, session, symbolBuilder)
            }
        }
        return getSymbolsByResolvable(fir, expression, session, symbolBuilder)
    }

    private fun getSymbolsForPackageDirective(
        expression: KtSimpleNameExpression,
        symbolBuilder: KtSymbolByFirBuilder
    ): List<KtFirPackageSymbol> {
        return listOfNotNull(getPackageSymbolFor(expression, symbolBuilder, forQualifiedType = false))
    }


    private fun getSymbolByResolvedNameReference(
        fir: FirResolvedNamedReference,
        expression: KtSimpleNameExpression,
        analysisSession: KtFirAnalysisSession,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        val parentAsCall = expression.parent as? KtCallExpression
        if (parentAsCall != null) {
            val firResolvable = parentAsCall.getOrBuildFirSafe<FirResolvable>(analysisSession.firResolveSession)
            if (firResolvable != null) {
                return getSymbolsByResolvable(firResolvable, expression, session, symbolBuilder)
            }
        }
        return fir.toTargetSymbol(session, symbolBuilder)
    }

    private fun KtSimpleNameExpression.isSyntheticOperatorReference() = when (this) {
        is KtOperationReferenceExpression -> operationSignTokenType in syntheticTokenTypes
        else -> false
    }

    private fun getSymbolsByVariableAssignment(
        fir: FirVariableAssignment,
        expression: KtSimpleNameExpression,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder,
    ): Collection<KtSymbol> {
        if (expression is KtNameReferenceExpression) {
            return fir.calleeReference?.toTargetSymbol(session, symbolBuilder) ?: emptyList()
        }

        val assignmentRValue = fir.rValue
        if (expression is KtOperationReferenceExpression &&
            assignmentRValue.source?.kind is KtFakeSourceElementKind.DesugaredCompoundAssignment
        ) {
            require(assignmentRValue is FirResolvable) {
                "Rvalue of desugared compound assignment should be resolvable, but it was ${assignmentRValue::class}"
            }

            return assignmentRValue.calleeReference.toTargetSymbol(session, symbolBuilder)
        }

        return emptyList()
    }

    private fun getSymbolsByNameArgumentExpression(
        expression: KtSimpleNameExpression,
        analysisSession: KtFirAnalysisSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        val ktValueArgumentName = expression.parent as? KtValueArgumentName ?: return emptyList()
        val ktValueArgument = ktValueArgumentName.parent as? KtValueArgument ?: return emptyList()
        val ktValueArgumentList = ktValueArgument.parent as? KtValueArgumentList ?: return emptyList()
        val ktCallExpression = ktValueArgumentList.parent as? KtCallElement ?: return emptyList()

        val firCall = ktCallExpression.getOrBuildFirSafe<FirCall>(analysisSession.firResolveSession) ?: return emptyList()
        val parameter = firCall.findCorrespondingParameter(ktValueArgument) ?: return emptyList()
        return listOfNotNull(parameter.buildSymbol(symbolBuilder))
    }

    private fun FirCall.findCorrespondingParameter(ktValueArgument: KtValueArgument): FirValueParameter? =
        resolvedArgumentMapping?.entries?.firstNotNullOfOrNull { (firArgument, firParameter) ->
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
            val parentFir = selectorExpression.getOrBuildFir(analysisSession.firResolveSession)
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
        // If the cursor position is on the label of `super`, we want to resolve to the current class. FIR represents `super` as
        // accessing the `super` property on `this`, hence this weird looking if condition. In addition, the current class type is available
        // from the dispatch receiver `this`.
        if (expression is KtLabelReferenceExpression && fir is FirPropertyAccessExpression && fir.calleeReference is FirSuperReference) {
            return listOfNotNull(fir.dispatchReceiver?.resolvedType?.toTargetSymbol(session, symbolBuilder))
        }
        val receiverOrImplicitInvoke = if (fir is FirImplicitInvokeCall) {
            fir.explicitReceiver?.unwrapSmartcastExpression()
        } else {
            null
        }

        return if (receiverOrImplicitInvoke is FirResolvedQualifier) {
            getSymbolsForResolvedQualifier(receiverOrImplicitInvoke, expression, session, symbolBuilder)
        } else {
            val calleeReference = (receiverOrImplicitInvoke as? FirQualifiedAccessExpression)?.calleeReference ?: fir.calleeReference
            calleeReference.toTargetSymbol(session, symbolBuilder, isInLabelReference = expression is KtLabelReferenceExpression)
        }
    }


    private fun getSymbolsByErrorNamedReference(
        fir: FirErrorNamedReference,
        symbolBuilder: KtSymbolByFirBuilder
    ): List<KtSymbol> =
        fir.getCandidateSymbols().map { it.fir.buildSymbol(symbolBuilder) }

    private fun getSymbolsByReturnExpression(
        expression: KtSimpleNameExpression,
        fir: FirReturnExpression,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        return if (expression is KtLabelReferenceExpression) {
            val labeledElement = fir.target.labeledElement
            if (labeledElement is FirErrorFunction) emptyList() else listOf(labeledElement.buildSymbol(symbolBuilder))
        } else emptyList()
    }

    private fun getSymbolsByFirFile(
        symbolBuilder: KtSymbolByFirBuilder,
        fir: FirFile
    ): List<KtSymbol> {
        return listOf(symbolBuilder.buildSymbol(fir.symbol))
    }

    private fun getSybmolsByTypeParameter(
        symbolBuilder: KtSymbolByFirBuilder,
        fir: FirTypeParameter
    ): List<KtSymbol> {
        return listOf(symbolBuilder.buildSymbol(fir.symbol))
    }

    private fun getSymbolsByResolvedReifiedTypeParameterReference(
        symbolBuilder: KtSymbolByFirBuilder,
        fir: FirResolvedReifiedParameterReference
    ): List<KtSymbol> {
        return listOf(symbolBuilder.buildSymbol(fir.symbol))
    }


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
        if (!expression.isPartOfUserTypeRefQualifier()) {
            return listOfNotNull(fir.toTargetSymbol(session, symbolBuilder))
        }

        if (fir is FirErrorTypeRef) {
            tryGettingSymbolFromPartiallyResolvedType(fir, expression, session, symbolBuilder).ifNotEmpty { return this }
            return listOfNotNull(getPackageSymbolFor(expression, symbolBuilder, forQualifiedType = true))
        }

        fun unwrapType(type: PsiElement?): KtTypeElement = when (type) {
            is KtTypeReference -> unwrapType(type.typeElement)
            is KtNullableType -> type.innerType
            is KtTypeElement -> type
            is KtNameReferenceExpression -> unwrapType(type.parent)
            else -> unexpectedElementError<PsiElement>(type)
        } ?: unexpectedElementError<PsiElement>(type)

        val ktTypeElementFromFirType = unwrapType(fir.psi)

        val classifiersToSkip = expression.parents.takeWhile { it != ktTypeElementFromFirType }.count()
        var classifier: FirClassLikeSymbol<*>? = fir.type.toRegularClassSymbol(session)
        repeat(classifiersToSkip) {
            classifier = classifier?.getContainingClassSymbol(session)
        }

        val firClassSymbol = classifier
            ?: return listOfNotNull(getPackageSymbolFor(expression, symbolBuilder, forQualifiedType = true))
        return listOf(symbolBuilder.classifierBuilder.buildClassifierSymbol(firClassSymbol))
    }

    private tailrec fun tryGettingSymbolFromPartiallyResolvedType(
        typeRef: FirTypeRef?,
        expression: KtSimpleNameExpression,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder,
    ): Collection<KtSymbol> {
        return when (typeRef) {
            null -> emptyList()
            is FirErrorTypeRef -> {
                tryGettingSymbolFromPartiallyResolvedType(
                    typeRef.partiallyResolvedTypeRef,
                    expression,
                    session,
                    symbolBuilder,
                )
            }
            is FirResolvedTypeRef -> {
                return getSymbolsForResolvedTypeRef(typeRef, expression, session, symbolBuilder)
            }
            else -> throwUnexpectedFirElementError(
                typeRef,
                ktElement = null,
                FirErrorTypeRef::class,
                FirResolvedTypeRef::class
            )
        }
    }

    private fun getSymbolsForResolvedQualifier(
        fir: FirResolvedQualifier,
        expression: KtSimpleNameExpression,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        val referencedSymbol = if (fir.resolvedToCompanionObject) {
            (fir.symbol?.fir as? FirRegularClass)?.companionObjectSymbol
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
        val referencedSymbolsByFir = listOfNotNull(symbolBuilder.buildSymbol(referencedSymbol))
        val fullQualifiedAccess = when (val psi = fir.source.psi) {
            // for cases like `Foo.Bar()`, where `Foo.Bar` is an object, and `Foo.Bar()` is a call to `invoke` operator
            is KtSimpleNameExpression -> psi.getQualifiedElement()
            else -> psi
        }
        if (fullQualifiedAccess !is KtDotQualifiedExpression) return referencedSymbolsByFir

        // When the source of an `FirResolvedQualifier` is a KtDotQualifiedExpression, we need to manually break up the qualified access and
        // resolve individual parts of it because in FIR, the entire qualified access is one element.
        if (referencedClass.isLocal) {
            // TODO: handle local classes after KT-47135 is fixed
            return referencedSymbolsByFir
        } else {
            var qualifiedAccess: KtDotQualifiedExpression = fullQualifiedAccess
            val referencedClassId =
                if ((referencedClass as? FirRegularClass)?.isCompanion == true) {
                    val deepestQualifier = qualifiedAccess.selectorExpression?.referenceExpression() as? KtNameReferenceExpression

                    // If we're looking for the deepest qualifier, then just resolve to the companion
                    if (expression === deepestQualifier) return referencedSymbolsByFir

                    if (fir.resolvedToCompanionObject) {
                        // this flag is true only when companion object is resolved through its containing class name,
                        // so we want to drop companion object own name from the classId
                        referencedClass.classId.outerClassId ?: return referencedSymbolsByFir
                    } else {
                        referencedClass.classId // ?: return referencedSymbolsByFir
                    }
                } else {
                    referencedClass.classId
                }
            val qualifiedAccessSegments = qualifiedAccess.fqNameSegments() ?: return referencedSymbolsByFir

            fun referencedClassIdAndQualifiedAccessMatch(
                qualifiedAccessSegments: List<String>
            ): Boolean {
                val referencedClassIdSegments =
                    referencedClassId.asSingleFqName().pathSegments()
                        .takeLast(qualifiedAccessSegments.size)
                        .map { it.identifierOrNullIfSpecial }
                return referencedClassIdSegments == qualifiedAccessSegments
            }

            if (!referencedClassIdAndQualifiedAccessMatch(qualifiedAccessSegments)) {
                // Referenced ClassId and qualified access (from source PSI) could be not identical if an import alias is involved.
                // E.g., test.pkg.R.string.hello v.s. coreR.string.hello where test.pkg.R is imported as coreR
                // Since an import alias ends with a simple identifier (i.e., can't be non-trivial dotted qualifier), we can safely assume
                // that the first segment of the qualified access could be the import alias if any. Then, we can still compare the
                // remaining parts.
                // E.g., coreR.string.hello
                //   -> string.hello (drop the first segment)
                //   test.pkg.R.string.hello
                //   -> string.hello (take last two segments, where the size is determined by the size of qualified access minus 1)
                val fixedQualifiedAccessSegments = qualifiedAccessSegments.drop(1)
                assert(referencedClassIdAndQualifiedAccessMatch(fixedQualifiedAccessSegments)) {
                    "Referenced classId $referencedClassId should end with qualifiedAccess expression ${qualifiedAccess.text} "
                }
            }

            // In the code below, we always maintain the contract that `classId` and `qualifiedAccess` should stay "in-sync", i.e. they
            // refer to the same class and classId should be null if `qualifiedAccess` references to a package.
            var classId: ClassId? = referencedClassId

            // Handle nested classes.
            while (classId != null) {
                if (expression === qualifiedAccess.selectorExpression?.referenceExpression()) {
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
        val qualifiers = generateSequence(this as KtExpression) { (it as? KtDotQualifiedExpression)?.receiverExpression }
            .map { (it as? KtDotQualifiedExpression)?.selectorExpression ?: it }
            .toList()
            .asReversed()

        val qualifyingReferences = qualifiers.mapIndexed { index, qualifier ->
            // We want to handle qualified calls like `foo.Bar.Baz()`, but not like `foo.Bar().Baz()`
            if (qualifier is KtCallExpression && index != qualifiers.lastIndex) return null

            qualifier.referenceExpression() as? KtNameReferenceExpression ?: return null
        }

        return qualifyingReferences.map { it.getReferencedName() }
    }

    private fun getSymbolsForAnnotationCall(
        fir: FirAnnotation,
        session: FirSession,
        symbolBuilder: KtSymbolByFirBuilder
    ): Collection<KtSymbol> {
        val type = fir.resolvedType
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

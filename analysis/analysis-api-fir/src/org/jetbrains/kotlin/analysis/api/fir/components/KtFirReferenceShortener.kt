/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.analysis.api.components.KtReferenceShortener
import org.jetbrains.kotlin.analysis.api.components.ShortenCommand
import org.jetbrains.kotlin.analysis.api.components.ShortenOption
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.analysis.api.fir.utils.computeImportableName
import org.jetbrains.kotlin.analysis.api.impl.barebone.parentsOfType
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.expressions.FirErrorResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnmatchedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.PackageResolutionResult
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.utils.addIfNotNull

internal class KtFirReferenceShortener(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
    override val firResolveState: FirModuleResolveState,
) : KtReferenceShortener(), KtFirAnalysisSessionComponent {
    private val context = FirShorteningContext(firResolveState)

    override fun collectShortenings(
        file: KtFile,
        selection: TextRange,
        classShortenOption: (KtClassLikeSymbol) -> ShortenOption,
        callableShortenOption: (KtCallableSymbol) -> ShortenOption
    ): ShortenCommand {
        val declarationToVisit = file.findSmallestDeclarationContainingSelection(selection)
            ?: file.withDeclarationsResolvedToBodyResolve()

        val firDeclaration = declarationToVisit.getOrBuildFirOfType<FirDeclaration>(firResolveState)

        val towerContext =
            LowLevelFirApiFacadeForResolveOnAir.onAirGetTowerContextProvider(firResolveState, declarationToVisit)

        //TODO: collect all usages of available symbols in the file and prevent importing symbols that could introduce name clashes, which
        // may alter the meaning of existing code.
        val collector = ElementsToShortenCollector(
            context,
            towerContext,
            selection,
            classShortenOption = { classShortenOption(analysisSession.firSymbolBuilder.buildSymbol(it.fir) as KtClassLikeSymbol) },
            callableShortenOption = { callableShortenOption(analysisSession.firSymbolBuilder.buildSymbol(it.fir) as KtCallableSymbol) },
            firResolveState,
        )
        firDeclaration.accept(collector)

        return ShortenCommandImpl(
            file,
            collector.namesToImport.distinct(),
            collector.namesToImportWithStar.distinct(),
            collector.typesToShorten.distinct().map { it.createSmartPointer() },
            collector.qualifiersToShorten.distinct().map { it.createSmartPointer() }
        )
    }

    private fun KtFile.withDeclarationsResolvedToBodyResolve(): KtFile {
        for (declaration in declarations) {
            declaration.getOrBuildFir(firResolveState) // temporary hack, resolves declaration to BODY_RESOLVE stage
        }

        return this
    }
}

private fun KtFile.findSmallestDeclarationContainingSelection(selection: TextRange): KtDeclaration? =
    findElementAt(selection.startOffset)
        ?.parentsOfType<KtDeclaration>(withSelf = true)
        ?.firstOrNull { selection in it.textRange }

/**
 * How a symbol is imported. The order of the enum entry represents the priority of imports. If a symbol is available from multiple kinds of
 * imports, the symbol from "smaller" kind is used. For example, an explicitly imported symbol can overwrite a star-imported symbol.
 */
private enum class ImportKind {
    /** The symbol is available from the local scope and hence cannot be imported or overwritten. */
    LOCAL,

    /** Explicitly imported by user. */
    EXPLICIT,

    /** Explicitly imported by Kotlin default. For example, `kotlin.String`. */
    DEFAULT_EXPLICIT,

    /** Implicitly imported from package. */
    PACKAGE,

    /** Star imported (star import) by user. */
    STAR,

    /** Star imported (star import) by Kotlin default. */
    DEFAULT_STAR;

    infix fun hasHigherPriorityThan(that: ImportKind): Boolean = this < that

    val canBeOverwrittenByExplicitImport: Boolean get() = DEFAULT_EXPLICIT hasHigherPriorityThan this

    companion object {
        fun fromScope(scope: FirScope): ImportKind {
            return when (scope) {
                is FirDefaultStarImportingScope -> DEFAULT_STAR
                is FirAbstractStarImportingScope -> STAR
                is FirPackageMemberScope -> PACKAGE
                is FirDefaultSimpleImportingScope -> DEFAULT_EXPLICIT
                is FirAbstractSimpleImportingScope -> EXPLICIT
                else -> LOCAL
            }
        }
    }
}

private data class AvailableSymbol<out T>(
    val symbol: T,
    val importKind: ImportKind,
)

private class FirShorteningContext(val firResolveState: FirModuleResolveState) {

    private val firSession: FirSession
        get() = firResolveState.rootModuleSession

    fun findFirstClassifierInScopesByName(positionScopes: List<FirScope>, targetClassName: Name): AvailableSymbol<ClassId>? {
        for (scope in positionScopes) {
            val classifierSymbol = scope.findFirstClassifierByName(targetClassName) ?: continue
            val classifierLookupTag = classifierSymbol.toLookupTag() as? ConeClassLikeLookupTag ?: continue

            return AvailableSymbol(classifierLookupTag.classId, ImportKind.fromScope(scope))
        }

        return null
    }

    fun findFunctionsInScopes(scopes: List<FirScope>, name: Name): List<AvailableSymbol<FirNamedFunctionSymbol>> {
        return scopes.flatMap { scope ->
            val importKind = ImportKind.fromScope(scope)
            scope.getFunctions(name).map {
                AvailableSymbol(it, importKind)
            }
        }
    }

    fun findPropertiesInScopes(scopes: List<FirScope>, name: Name): List<AvailableSymbol<FirVariableSymbol<*>>> {
        return scopes.flatMap { scope ->
            val importKind = ImportKind.fromScope(scope)
            scope.getProperties(name).map {
                AvailableSymbol(it, importKind)
            }
        }
    }

    private fun FirScope.findFirstClassifierByName(name: Name): FirClassifierSymbol<*>? {
        var element: FirClassifierSymbol<*>? = null

        processClassifiersByName(name) {
            if (element == null) {
                element = it
            }
        }

        return element
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun findScopesAtPosition(
        position: KtElement,
        newImports: List<FqName>,
        towerContextProvider: FirTowerContextProvider
    ): List<FirScope>? {
        val towerDataContext = towerContextProvider.getClosestAvailableParentContext(position) ?: return null
        val result = buildList {
            addAll(towerDataContext.nonLocalTowerDataElements.mapNotNull { it.scope })
            addIfNotNull(createFakeImportingScope(newImports))
            addAll(towerDataContext.localScopes)
        }

        return result.asReversed()
    }

    private fun createFakeImportingScope(newImports: List<FqName>): FirScope? {
        val resolvedNewImports = newImports.mapNotNull { createFakeResolvedImport(it) }
        if (resolvedNewImports.isEmpty()) return null

        return FirExplicitSimpleImportingScope(resolvedNewImports, firSession, ScopeSession())
    }

    private fun createFakeResolvedImport(fqNameToImport: FqName): FirResolvedImport? {
        val packageOrClass =
            (resolveToPackageOrClass(firSession.symbolProvider, fqNameToImport) as? PackageResolutionResult.PackageOrClass) ?: return null

        val delegateImport = buildImport {
            importedFqName = fqNameToImport
            isAllUnder = false
        }

        return buildResolvedImport {
            delegate = delegateImport
            packageFqName = packageOrClass.packageFqName
        }
    }

    fun getRegularClass(typeRef: FirTypeRef): FirRegularClass? {
        return typeRef.toRegularClassSymbol(firSession)?.fir
    }

    fun toClassSymbol(classId: ClassId) =
        firSession.symbolProvider.getClassLikeSymbolByClassId(classId)

    fun convertToImportableName(callableSymbol: FirCallableSymbol<*>): FqName? =
        callableSymbol.computeImportableName(firSession)
}

private sealed class ElementToShorten {
    abstract val nameToImport: FqName?
    abstract val importAllInParent: Boolean
}

private class ShortenType(
    val element: KtUserType,
    override val nameToImport: FqName? = null,
    override val importAllInParent: Boolean = false
) : ElementToShorten()

private class ShortenQualifier(
    val element: KtDotQualifiedExpression,
    override val nameToImport: FqName? = null,
    override val importAllInParent: Boolean = false
) : ElementToShorten()

private class ElementsToShortenCollector(
    private val shorteningContext: FirShorteningContext,
    private val towerContextProvider: FirTowerContextProvider,
    private val selection: TextRange,
    private val classShortenOption: (FirClassLikeSymbol<*>) -> ShortenOption,
    private val callableShortenOption: (FirCallableSymbol<*>) -> ShortenOption,
    private val firResolveState: FirModuleResolveState,
) :
    FirVisitorVoid() {
    val namesToImport: MutableList<FqName> = mutableListOf()
    val namesToImportWithStar: MutableList<FqName> = mutableListOf()
    val typesToShorten: MutableList<KtUserType> = mutableListOf()
    val qualifiersToShorten: MutableList<KtDotQualifiedExpression> = mutableListOf()
    private val visitedProperty = mutableSetOf<FirProperty>()

    override fun visitValueParameter(valueParameter: FirValueParameter) {
        super.visitValueParameter(valueParameter)
        valueParameter.correspondingProperty?.let { visitProperty(it) }
    }

    override fun visitProperty(property: FirProperty) {
        if (visitedProperty.add(property)) {
            super.visitProperty(property)
        }
    }

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
        visitResolvedTypeRef(errorTypeRef)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        processTypeRef(resolvedTypeRef)

        resolvedTypeRef.acceptChildren(this)
        resolvedTypeRef.delegatedTypeRef?.accept(this)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        super.visitResolvedQualifier(resolvedQualifier)

        processTypeQualifier(resolvedQualifier)
    }

    override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier) {
        super.visitErrorResolvedQualifier(errorResolvedQualifier)

        processTypeQualifier(errorResolvedQualifier)
    }

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        super.visitResolvedNamedReference(resolvedNamedReference)

        processPropertyReference(resolvedNamedReference)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        super.visitFunctionCall(functionCall)

        processFunctionCall(functionCall)
    }

    private fun processTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        val wholeTypeReference = resolvedTypeRef.psi as? KtTypeReference ?: return
        if (!wholeTypeReference.textRange.intersects(selection)) return

        val wholeClassifierId = resolvedTypeRef.type.lowerBoundIfFlexible().candidateClassId ?: return
        val wholeTypeElement = wholeTypeReference.typeElement?.unwrapNullability() as? KtUserType ?: return

        if (wholeTypeElement.qualifier == null) return

        findTypeToShorten(wholeClassifierId, wholeTypeElement)?.let(::addElementToShorten)
    }

    val ConeKotlinType.candidateClassId: ClassId?
        get() {
            return when (this) {
                is ConeClassErrorType -> when (val diagnostic = this.diagnostic) {
                    // Tolerate code that misses type parameters while shortening it.
                    is ConeUnmatchedTypeArgumentsError -> diagnostic.candidateSymbol.classId
                    else -> null
                }
                is ConeClassLikeType -> lookupTag.classId
                else -> null
            }
        }

    private fun findTypeToShorten(wholeClassifierId: ClassId, wholeTypeElement: KtUserType): ElementToShorten? {
        val positionScopes = shorteningContext.findScopesAtPosition(wholeTypeElement, namesToImport, towerContextProvider) ?: return null
        val allClassIds = wholeClassifierId.outerClassesWithSelf
        val allQualifiedTypeElements = wholeTypeElement.qualifiedTypesWithSelf
        return findClassifierElementsToShorten(
            positionScopes,
            allClassIds,
            allQualifiedTypeElements,
            ::ShortenType,
            this::findFakePackageToShorten
        )
    }

    private fun findFakePackageToShorten(typeElement: KtUserType): ShortenType? {
        val deepestTypeWithQualifier = typeElement.qualifiedTypesWithSelf.last()

        return if (deepestTypeWithQualifier.hasFakeRootPrefix()) ShortenType(deepestTypeWithQualifier) else null
    }

    private fun processTypeQualifier(resolvedQualifier: FirResolvedQualifier) {
        val wholeClassQualifier = resolvedQualifier.classId ?: return
        val qualifierPsi = resolvedQualifier.psi ?: return
        if (!qualifierPsi.textRange.intersects(selection)) return
        val wholeQualifierElement = when (qualifierPsi) {
            is KtDotQualifiedExpression -> qualifierPsi
            is KtNameReferenceExpression -> qualifierPsi.getDotQualifiedExpressionForSelector() ?: return
            else -> return
        }

        findTypeQualifierToShorten(wholeClassQualifier, wholeQualifierElement)?.let(::addElementToShorten)
    }

    private fun findTypeQualifierToShorten(
        wholeClassQualifier: ClassId,
        wholeQualifierElement: KtDotQualifiedExpression
    ): ElementToShorten? {
        val positionScopes: List<FirScope> =
            shorteningContext.findScopesAtPosition(wholeQualifierElement, namesToImport, towerContextProvider) ?: return null
        val allClassIds: Sequence<ClassId> = wholeClassQualifier.outerClassesWithSelf
        val allQualifiers: Sequence<KtDotQualifiedExpression> = wholeQualifierElement.qualifiedExpressionsWithSelf
        return findClassifierElementsToShorten(
            positionScopes,
            allClassIds,
            allQualifiers,
            ::ShortenQualifier,
            this::findFakePackageToShorten
        )
    }

    private inline fun <E> findClassifierElementsToShorten(
        positionScopes: List<FirScope>,
        allClassIds: Sequence<ClassId>,
        allQualifiedElements: Sequence<E>,
        createElementToShorten: (E, nameToImport: FqName?, importAllInParent: Boolean) -> ElementToShorten,
        findFakePackageToShortenFn: (E) -> ElementToShorten?,
    ): ElementToShorten? {

        for ((classId, element) in allClassIds.zip(allQualifiedElements)) {
            val option = classShortenOption(shorteningContext.toClassSymbol(classId) ?: return null)
            if (option == ShortenOption.DO_NOT_SHORTEN) continue

            // Find class with the same name that's already available in this file.
            val availableClassifier = shorteningContext.findFirstClassifierInScopesByName(positionScopes, classId.shortClassName)

            when {
                // No class with name `classId.shortClassName` is present in the scope. Hence, we can safely import the name and shorten
                // the reference.
                availableClassifier == null -> {
                    // Caller indicates don't shorten if doing that needs importing more names. Hence, we just skip.
                    if (option == ShortenOption.SHORTEN_IF_ALREADY_IMPORTED) continue
                    return createElementToShorten(
                        element,
                        classId.asSingleFqName(),
                        option == ShortenOption.SHORTEN_AND_STAR_IMPORT
                    )
                }
                // The class with name `classId.shortClassName` happens to be the same class referenced by this qualified access.
                availableClassifier.symbol == classId -> {
                    // Respect caller's request to use star import, if it's not already star-imported.
                    return when {
                        availableClassifier.importKind == ImportKind.EXPLICIT && option == ShortenOption.SHORTEN_AND_STAR_IMPORT -> {
                            createElementToShorten(element, classId.asSingleFqName(), true)
                        }
                        // Otherwise, just shorten it and don't alter import statements
                        else -> createElementToShorten(element, null, false)
                    }
                }
                // Allow using star import to overwrite members implicitly imported by default.
                availableClassifier.importKind == ImportKind.DEFAULT_STAR && option == ShortenOption.SHORTEN_AND_STAR_IMPORT -> {
                    return createElementToShorten(element, classId.asSingleFqName(), true)
                }
                // Allow using explicit import to overwrite members star-imported or in package
                availableClassifier.importKind.canBeOverwrittenByExplicitImport && option == ShortenOption.SHORTEN_AND_IMPORT -> {
                    return createElementToShorten(element, classId.asSingleFqName(), false)
                }
            }
        }
        return findFakePackageToShortenFn(allQualifiedElements.last())
    }

    private fun processPropertyReference(resolvedNamedReference: FirResolvedNamedReference) {
        val referenceExpression = resolvedNamedReference.psi as? KtNameReferenceExpression ?: return
        if (!referenceExpression.textRange.intersects(selection)) return
        val qualifiedProperty = referenceExpression.getDotQualifiedExpressionForSelector() ?: return

        val callableSymbol = resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*> ?: return
        processCallableQualifiedAccess(callableSymbol, qualifiedProperty, qualifiedProperty, shorteningContext::findPropertiesInScopes)
    }

    private fun processFunctionCall(functionCall: FirFunctionCall) {
        if (!canBePossibleToDropReceiver(functionCall)) return

        val qualifiedCallExpression = functionCall.psi as? KtDotQualifiedExpression ?: return
        if (!qualifiedCallExpression.textRange.intersects(selection)) return
        val callExpression = qualifiedCallExpression.selectorExpression as? KtCallExpression ?: return

        val calleeReference = functionCall.calleeReference
        val calledSymbol = findUnambiguousReferencedCallableId(calleeReference) ?: return
        processCallableQualifiedAccess(calledSymbol, qualifiedCallExpression, callExpression, shorteningContext::findFunctionsInScopes)
    }

    private fun processCallableQualifiedAccess(
        calledSymbol: FirCallableSymbol<*>,
        qualifiedCallExpression: KtDotQualifiedExpression,
        expressionToGetScope: KtExpression,
        findCallableInScopes: (List<FirScope>, Name) -> List<AvailableSymbol<FirCallableSymbol<*>>>,
    ) {
        val option = callableShortenOption(calledSymbol)
        if (option == ShortenOption.DO_NOT_SHORTEN) return

        val scopes = shorteningContext.findScopesAtPosition(expressionToGetScope, namesToImport, towerContextProvider) ?: return
        val availableCallables = findCallableInScopes(scopes, calledSymbol.name)

        val nameToImport = shorteningContext.convertToImportableName(calledSymbol)

        val (matchedCallables, otherCallables) = availableCallables.partition { it.symbol.callableId == calledSymbol.callableId }
        val callToShorten = when {
            // TODO: instead of allowing import only if the other callables are all with kind `DEFAULT_STAR`, we should allow import if
            //  the requested import kind has higher priority than the available symbols.
            otherCallables.all { it.importKind == ImportKind.DEFAULT_STAR } -> {
                when {
                    matchedCallables.isEmpty() -> {
                        if (nameToImport == null || option == ShortenOption.SHORTEN_IF_ALREADY_IMPORTED) return
                        ShortenQualifier(
                            qualifiedCallExpression,
                            nameToImport,
                            importAllInParent = option == ShortenOption.SHORTEN_AND_STAR_IMPORT
                        )
                    }
                    // Respect caller's request to star import this symbol.
                    matchedCallables.any { it.importKind == ImportKind.EXPLICIT } && option == ShortenOption.SHORTEN_AND_STAR_IMPORT ->
                        ShortenQualifier(qualifiedCallExpression, nameToImport, importAllInParent = true)
                    else -> ShortenQualifier(qualifiedCallExpression)
                }
            }
            else -> findFakePackageToShorten(qualifiedCallExpression)
        }

        callToShorten?.let(::addElementToShorten)
    }

    private fun canBePossibleToDropReceiver(functionCall: FirFunctionCall): Boolean {
        // we can remove receiver only if it is a qualifier
        val explicitReceiver = functionCall.explicitReceiver as? FirResolvedQualifier ?: return false

        // if there is no extension receiver necessary, then it can be removed
        if (functionCall.extensionReceiver is FirNoReceiverExpression) return true

        val receiverType = shorteningContext.getRegularClass(explicitReceiver.typeRef) ?: return true
        return receiverType.classKind != ClassKind.OBJECT
    }

    private fun findUnambiguousReferencedCallableId(namedReference: FirNamedReference): FirCallableSymbol<*>? {
        val unambiguousSymbol = when (namedReference) {
            is FirResolvedNamedReference -> namedReference.resolvedSymbol
            is FirErrorNamedReference -> {
                val candidateSymbol = namedReference.candidateSymbol
                if (candidateSymbol !is FirErrorFunctionSymbol) {
                    candidateSymbol
                } else {
                    getSingleUnambiguousCandidate(namedReference)
                }
            }
            else -> null
        }

        return (unambiguousSymbol as? FirCallableSymbol<*>)
    }

    /**
     * If [namedReference] is ambiguous and all candidates point to the callables with same callableId,
     * returns the first candidate; otherwise returns null.
     */
    private fun getSingleUnambiguousCandidate(namedReference: FirErrorNamedReference): FirCallableSymbol<*>? {
        val coneAmbiguityError = namedReference.diagnostic as? ConeAmbiguityError ?: return null

        val candidates = coneAmbiguityError.candidates.map { it.symbol as FirCallableSymbol<*> }
        require(candidates.isNotEmpty()) { "Cannot have zero candidates" }

        val distinctCandidates = candidates.distinctBy { it.callableId }
        return distinctCandidates.singleOrNull()
            ?: error("Expected all candidates to have same callableId, but got: ${distinctCandidates.map { it.callableId }}")
    }

    private fun findFakePackageToShorten(wholeQualifiedExpression: KtDotQualifiedExpression): ShortenQualifier? {
        val deepestQualifier = wholeQualifiedExpression.qualifiedExpressionsWithSelf.last()
        return if (deepestQualifier.hasFakeRootPrefix()) ShortenQualifier(deepestQualifier) else null
    }

    private fun addElementToShorten(element: ElementToShorten) {
        if (element.importAllInParent && element.nameToImport?.parentOrNull()?.isRoot == false) {
            namesToImportWithStar.addIfNotNull(element.nameToImport?.parent())
        } else {
            namesToImport.addIfNotNull(element.nameToImport)
        }
        when (element) {
            is ShortenType -> {
                typesToShorten.add(element.element)
            }
            is ShortenQualifier -> {
                qualifiersToShorten.add(element.element)
            }
        }
    }

    private val ClassId.outerClassesWithSelf: Sequence<ClassId>
        get() = generateSequence(this) { it.outerClassId }

    /**
     * Note: The resulting sequence does not contain non-qualified types!
     *
     * For type `A.B.C.D` it will return sequence of [`A.B.C.D`, `A.B.C`, `A.B`] (**without** `A`).
     */
    private val KtUserType.qualifiedTypesWithSelf: Sequence<KtUserType>
        get() {
            require(qualifier != null) {
                "Type element should have at least one qualifier, instead it was $text"
            }

            return generateSequence(this) { it.qualifier }.takeWhile { it.qualifier != null }
        }

    private val KtDotQualifiedExpression.qualifiedExpressionsWithSelf: Sequence<KtDotQualifiedExpression>
        get() = generateSequence(this) { it.receiverExpression as? KtDotQualifiedExpression }
}

private class ShortenCommandImpl(
    val targetFile: KtFile,
    val importsToAdd: List<FqName>,
    val starImportsToAdd: List<FqName>,
    val typesToShorten: List<SmartPsiElementPointer<KtUserType>>,
    val qualifiersToShorten: List<SmartPsiElementPointer<KtDotQualifiedExpression>>,
) : ShortenCommand {

    override fun invokeShortening() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        for (nameToImport in importsToAdd) {
            addImportToFile(targetFile.project, targetFile, nameToImport)
        }

        for (nameToImport in starImportsToAdd) {
            addImportToFile(targetFile.project, targetFile, nameToImport, allUnder = true)
        }

//todo
//        PostprocessReformattingAspect.getInstance(targetFile.project).disablePostprocessFormattingInside {
        for (typePointer in typesToShorten) {
            val type = typePointer.element ?: continue
            type.deleteQualifier()
        }

        for (callPointer in qualifiersToShorten) {
            val call = callPointer.element ?: continue
            call.deleteQualifier()
        }
//        }
    }

    override val isEmpty: Boolean get() = typesToShorten.isEmpty() && qualifiersToShorten.isEmpty()
}

private fun KtUserType.hasFakeRootPrefix(): Boolean =
    qualifier?.referencedName == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

private fun KtDotQualifiedExpression.hasFakeRootPrefix(): Boolean =
    (receiverExpression as? KtNameReferenceExpression)?.getReferencedName() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

internal fun KtElement.getDotQualifiedExpressionForSelector(): KtDotQualifiedExpression? =
    getQualifiedExpressionForSelector() as? KtDotQualifiedExpression

private fun KtDotQualifiedExpression.deleteQualifier(): KtExpression? {
    val selectorExpression = selectorExpression ?: return null
    return this.replace(selectorExpression) as KtExpression
}

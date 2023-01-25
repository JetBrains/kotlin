/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.analysis.api.components.KtReferenceShortener
import org.jetbrains.kotlin.analysis.api.components.ShortenCommand
import org.jetbrains.kotlin.analysis.api.components.ShortenOption
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.components.ElementsToShortenCollector.PartialOrderOfScope.Companion.toPartialOrder
import org.jetbrains.kotlin.analysis.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.analysis.api.fir.utils.computeImportableName
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.AllCandidatesResolver
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
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
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.utils.addIfNotNull

internal class KtFirReferenceShortener(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
    override val firResolveSession: LLFirResolveSession,
) : KtReferenceShortener(), KtFirAnalysisSessionComponent {
    private val context = FirShorteningContext(analysisSession)

    override fun collectShortenings(
        file: KtFile,
        selection: TextRange,
        classShortenOption: (KtClassLikeSymbol) -> ShortenOption,
        callableShortenOption: (KtCallableSymbol) -> ShortenOption
    ): ShortenCommand {
        val declarationToVisit = file.findSmallestDeclarationContainingSelection(selection)
            ?: file.withDeclarationsResolvedToBodyResolve()

        val firDeclaration = declarationToVisit.getOrBuildFir(firResolveSession) as? FirDeclaration ?: return ShortenCommandImpl(
            file.createSmartPointer(), emptyList(), emptyList(), emptyList(), emptyList(),
        )

        val towerContext =
            LowLevelFirApiFacadeForResolveOnAir.onAirGetTowerContextProvider(firResolveSession, declarationToVisit)

        //TODO: collect all usages of available symbols in the file and prevent importing symbols that could introduce name clashes, which
        // may alter the meaning of existing code.
        val collector = ElementsToShortenCollector(
            context,
            towerContext,
            selection,
            classShortenOption = { classShortenOption(analysisSession.firSymbolBuilder.buildSymbol(it) as KtClassLikeSymbol) },
            callableShortenOption = { callableShortenOption(analysisSession.firSymbolBuilder.buildSymbol(it) as KtCallableSymbol) },
            firResolveSession,
        )
        firDeclaration.accept(collector)

        return ShortenCommandImpl(
            file.createSmartPointer(),
            collector.namesToImport.distinct(),
            collector.namesToImportWithStar.distinct(),
            collector.typesToShorten.distinct().map { it.createSmartPointer() },
            collector.qualifiersToShorten.distinct().map { it.createSmartPointer() }
        )
    }

    private fun KtFile.withDeclarationsResolvedToBodyResolve(): KtFile {
        for (declaration in declarations) {
            declaration.getOrBuildFir(firResolveSession) // temporary hack, resolves declaration to BODY_RESOLVE stage
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

private class FirShorteningContext(val analysisSession: KtFirAnalysisSession) {
    private val firResolveSession = analysisSession.firResolveSession

    private val firSession: FirSession
        get() = firResolveSession.useSiteFirSession

    class ClassifierCandidate(val scope: FirScope, val availableSymbol: AvailableSymbol<ClassId>)

    fun findFirstClassifierInScopesByName(positionScopes: List<FirScope>, targetClassName: Name): AvailableSymbol<ClassId>? {
        for (scope in positionScopes) {
            val classifierSymbol = scope.findFirstClassifierByName(targetClassName) ?: continue
            val classifierLookupTag = classifierSymbol.toLookupTag() as? ConeClassLikeLookupTag ?: continue

            return AvailableSymbol(classifierLookupTag.classId, ImportKind.fromScope(scope))
        }

        return null
    }

    fun findClassifiersInScopesByName(scopes: List<FirScope>, targetClassName: Name): List<ClassifierCandidate> =
        scopes.mapNotNull { scope ->
            val classifierSymbol = scope.findFirstClassifierByName(targetClassName) ?: return@mapNotNull null
            val classifierLookupTag = classifierSymbol.toLookupTag() as? ConeClassLikeLookupTag ?: return@mapNotNull null

            ClassifierCandidate(scope, AvailableSymbol(classifierLookupTag.classId, ImportKind.fromScope(scope)))
        }

    fun findFunctionsInScopes(scopes: List<FirScope>, name: Name): List<AvailableSymbol<FirFunctionSymbol<*>>> {
        return scopes.flatMap { scope ->
            val importKind = ImportKind.fromScope(scope)
            buildList {
                // Collect constructors
                scope.findFirstClassifierByName(name)?.let { classifierSymbol ->
                    val classSymbol = classifierSymbol as? FirClassSymbol ?: return@let null
                    classSymbol.declarationSymbols.filterIsInstance<FirConstructorSymbol>()
                }?.forEach { add(AvailableSymbol(it, importKind)) }

                // Collect functions
                addAll(scope.getFunctions(name).map { AvailableSymbol(it, importKind) })
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

    fun findScopesAtPosition(
        position: KtElement,
        newImports: List<FqName>,
        towerContextProvider: FirTowerContextProvider
    ): List<FirScope>? {
        val towerDataContext = towerContextProvider.getClosestAvailableParentContext(position) ?: return null
        val result = buildList {
            addAll(towerDataContext.nonLocalTowerDataElements.mapNotNull {
                // We must use `it.getAvailableScope()` instead of `it.scope` to check scopes of companion objects as well.
                it.getAvailableScope()
            })
            addIfNotNull(createFakeImportingScope(newImports))
            addAll(towerDataContext.localScopes)
        }

        return result.asReversed()
    }

    private fun createFakeImportingScope(newImports: List<FqName>): FirScope? {
        val resolvedNewImports = newImports.mapNotNull { createFakeResolvedImport(it) }
        if (resolvedNewImports.isEmpty()) return null

        return FirExplicitSimpleImportingScope(
            resolvedNewImports,
            firSession,
            analysisSession.getScopeSessionFor(firSession),
        )
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
    private val firResolveSession: LLFirResolveSession,
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
                is ConeErrorType -> when (val diagnostic = this.diagnostic) {
                    // Tolerate code that misses type parameters while shortening it.
                    is ConeUnmatchedTypeArgumentsError -> diagnostic.symbol.classId
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

    /**
     * Returns true if the class symbol has a type parameter that is supposed to be provided for its parent class.
     *
     * Example:
     * class Outer<T> {
     *   inner class Inner // Inner has an implicit type parameter `T`.
     * }
     */
    private fun FirClassLikeSymbol<*>.hasTypeParameterFromParent(): Boolean = typeParameterSymbols.orEmpty().any {
        it.containingDeclarationSymbol != this
    }

    private fun FirScope.correspondingClassIdIfExists(): ClassId = when (this) {
        is FirNestedClassifierScope -> klass.classId
        is FirClassUseSiteMemberScope -> classId
        else -> error("FirScope `$this` is expected to be one of FirNestedClassifierScope and FirClassUseSiteMemberScope to get ClassId")
    }

    private fun ClassId.idWithoutCompanion() = if (shortClassName == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) outerClassId else this

    private fun FirScope.isScopeForClass() = this is FirNestedClassifierScope || this is FirClassUseSiteMemberScope

    /**
     * Assuming that both this [FirScope] and [another] are [FirNestedClassifierScope] or [FirClassUseSiteMemberScope] and both of them
     * are surrounding [from], returns whether this [FirScope] is closer than [another] based on the distance from [from].
     *
     * If one of this [FirScope] and [another] is not [FirNestedClassifierScope] or [FirClassUseSiteMemberScope], it returns false.
     *
     * Example:
     *   class Outer { // scope1  ClassId = Other
     *     class Inner { // scope2  ClassId = Other.Inner
     *       fun foo() {
     *         // Distance to scopes for classes from <element> in the order from the closest:
     *         //   scope2 -> scope3 -> scope1
     *         <element>
     *       }
     *       companion object { // scope3  ClassId = Other.Inner.Companion
     *       }
     *     }
     *   }
     *
     * This function determines the distance based on [ClassId].
     */
    private fun FirScope.isScopeForClassCloserThanAnotherScopeForClass(another: FirScope, from: KtClass): Boolean {
        // Make sure both are scopes for classes
        if (!isScopeForClass() || !another.isScopeForClass()) return false

        if (this == another) return false

        val classId = correspondingClassIdIfExists()
        val classIdOfAnother = another.correspondingClassIdIfExists()
        if (classId == classIdOfAnother) return false

        // Find the first ClassId matching inner class. If the first matching one is this scope's ClassId, it means this scope is closer
        // than `another`.
        val candidates = setOfNotNull(classId, classIdOfAnother, classId.idWithoutCompanion(), classIdOfAnother.idWithoutCompanion())
        val closestClassId = findMostInnerClassMatchingId(from, candidates)
        return closestClassId == classId || (closestClassId != classIdOfAnother && closestClassId == classId.idWithoutCompanion())
    }

    /**
     * Travels all containing classes of [innerClass] and finds the one matching ClassId with one of [candidates]. Returns the matching
     * ClassId. If it does not have a matching ClassId, it returns null.
     */
    private fun findMostInnerClassMatchingId(innerClass: KtClass, candidates: Set<ClassId>): ClassId? {
        var classInNestedClass: KtClass? = innerClass
        while (classInNestedClass != null) {
            val containingClassId = classInNestedClass.getClassId()
            if (containingClassId in candidates) return containingClassId
            classInNestedClass = classInNestedClass.containingClass()
        }
        return null
    }


    /**
     * An enum class to specify the distance of scopes from an element as a partial order
     *
     * Example:
     *   import ... // scope1  FirExplicitSimpleImportingScope - enum entry: ExplicitSimpleImporting
     *   // scope2  FirPackageMemberScope - enum entry: PackageMember
     *   class Outer { // scope3  FirClassUseSiteMemberScope - enum entry: ClassUseSite
     *     class Inner { // scope4  FirClassUseSiteMemberScope or FirNestedClassifierScope - enum entry: ClassUseSite/NestedClassifier
     *       fun foo() { // scope5  FirLocalScope - enum entry: Local
     *
     *         // Distance to scopes from <element> in the order from the closest:
     *         //   scope5 -> scope4 -> scope6 -> scope3 -> scope1 -> scope2
     *         <element>
     *
     *       }
     *       companion object {
     *         // scope6  FirClassUseSiteMemberScope or FirNestedClassifierScope - enum entry: ClassUseSite/NestedClassifier
     *       }
     *     }
     *   }
     */
    private enum class PartialOrderOfScope(
        val scopeDistanceLevel: Int // Note: Don't use the built-in ordinal since there are some scopes that are at the same level.
    ) {
        Local(1),
        ClassUseSite(2),
        NestedClassifier(2),
        ExplicitSimpleImporting(3),
        PackageMember(4),
        Unclassified(5),
        ;

        companion object {
            fun FirScope.toPartialOrder(): PartialOrderOfScope {
                return when (this) {
                    is FirLocalScope -> Local
                    is FirClassUseSiteMemberScope -> ClassUseSite
                    is FirNestedClassifierScope -> NestedClassifier
                    is FirExplicitSimpleImportingScope -> ExplicitSimpleImporting
                    is FirPackageMemberScope -> PackageMember
                    else -> Unclassified
                }
            }
        }
    }

    /**
     * Returns whether this [FirScope] is a scope wider than [another] based on the above [PartialOrderOfScope] or not.
     */
    private fun FirScope.isWiderThan(another: FirScope): Boolean =
        toPartialOrder().scopeDistanceLevel > another.toPartialOrder().scopeDistanceLevel

    /**
     * Assuming that all scopes in this List<FirScope> and [base] are surrounding [from], returns whether an element of
     * this List<FirScope> is closer than [base] based on the distance from [from].
     */
    private fun List<FirScope>.hasScopeCloserThan(base: FirScope, from: KtElement) = any { scope ->
        if (scope.isScopeForClass() && base.isScopeForClass()) {
            val classContainingFrom = from.containingClass() ?: return@any false
            return@any scope.isScopeForClassCloserThanAnotherScopeForClass(base, classContainingFrom)
        }
        base.isWiderThan(scope)
    }

    /**
     * Returns true if this [PsiFile] has a [KtImportDirective] whose imported FqName is the same as [classId] but references a different
     * symbol.
     */
    private fun PsiFile.hasImportDirectiveForDifferentSymbolWithSameName(classId: ClassId): Boolean {
        val importDirectivesWithSameImportedFqName = collectDescendantsOfType { importedDirective: KtImportDirective ->
            importedDirective.importedFqName?.shortName() == classId.shortClassName
        }
        return importDirectivesWithSameImportedFqName.isNotEmpty() &&
                importDirectivesWithSameImportedFqName.all { it.importedFqName != classId.asSingleFqName() }
    }

    private fun shortenClassifierIfAlreadyImported(
        classId: ClassId,
        element: KtElement,
        classSymbol: FirClassLikeSymbol<*>,
        positionScopes: List<FirScope>,
    ): Boolean {
        // If its parent has a type parameter, we cannot shorten it because it will lose its type parameter.
        if (classSymbol.hasTypeParameterFromParent()) return false

        val name = classId.shortClassName
        val availableClassifiers = shorteningContext.findClassifiersInScopesByName(positionScopes, name)
        val matchingAvailableSymbol = availableClassifiers.firstOrNull { it.availableSymbol.symbol == classId }
        val scopeForClass = matchingAvailableSymbol?.scope ?: return false

        if (availableClassifiers.map { it.scope }.hasScopeCloserThan(scopeForClass, element)) return false

        /**
         * If we have a property with the same name, avoid dropping qualifiers makes it reference a property with the same name e.g.,
         *    package my.component
         *    class foo { .. }  // A
         *    ..
         *    fun test() {
         *      val foo = ..    // B
         *      my.component.foo::class.java  // If we drop `my.component`, it will reference `B` instead of `A`
         *    }
         */
        if (shorteningContext.findPropertiesInScopes(positionScopes, name).isNotEmpty()) {
            val firForElement = element.getOrBuildFir(firResolveSession) as? FirQualifiedAccessExpression
            val typeArguments = firForElement?.typeArguments ?: emptyList()
            val qualifiedAccessCandidates = findCandidatesForPropertyAccess(classSymbol.annotations, typeArguments, name, element)
            if (qualifiedAccessCandidates.mapNotNull { it.candidate.originScope }.hasScopeCloserThan(scopeForClass, element)) return false
        }

        return !element.containingFile.hasImportDirectiveForDifferentSymbolWithSameName(classId)
    }

    private inline fun <E : KtElement> findClassifierElementsToShorten(
        positionScopes: List<FirScope>,
        allClassIds: Sequence<ClassId>,
        allQualifiedElements: Sequence<E>,
        createElementToShorten: (E, nameToImport: FqName?, importAllInParent: Boolean) -> ElementToShorten,
        findFakePackageToShortenFn: (E) -> ElementToShorten?,
    ): ElementToShorten? {
        for ((classId, element) in allClassIds.zip(allQualifiedElements)) {
            val classSymbol = shorteningContext.toClassSymbol(classId) ?: return null
            val option = classShortenOption(classSymbol)
            if (option == ShortenOption.DO_NOT_SHORTEN) continue

            if (shortenClassifierIfAlreadyImported(classId, element, classSymbol, positionScopes)) {
                return createElementToShorten(element, null, false)
            }
            if (option == ShortenOption.SHORTEN_IF_ALREADY_IMPORTED) continue

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

    private fun resolveUnqualifiedAccess(
        fullyQualifiedAccess: FirQualifiedAccessExpression,
        name: Name,
        expressionInScope: KtExpression,
    ): List<OverloadCandidate> {
        val fakeCalleeReference = buildSimpleNamedReference { this.name = name }
        val functionCall = fullyQualifiedAccess as? FirFunctionCall
        val fakeFirQualifiedAccess: FirQualifiedAccessExpression?
        if (functionCall == null) {
            fakeFirQualifiedAccess = buildPropertyAccessExpression {
                annotations.addAll(fullyQualifiedAccess.annotations)
                typeArguments.addAll(fullyQualifiedAccess.typeArguments)
                calleeReference = fakeCalleeReference
            }
        } else {
            val callExpression = expressionInScope as? KtCallExpression
            fakeFirQualifiedAccess = buildFunctionCall {
                annotations.addAll(functionCall.annotations)

                /**
                 * It is important to avoid passing type arguments when they are implicit type arguments.
                 * For example,
                 *   package a.b.c
                 *   fun <T, E> foo(a: T, b: E) {}      // A
                 *   fun test() {
                 *     fun foo(a: Int, b: String) {}      // B
                 *     a.b.c.foo(3, "test")
                 *   }
                 *
                 * In the above code, we must prevent it from shortening `a.b.c.foo(3, "test")`. However, if we explicitly pass the type
                 * arguments to the resolver, the resolver considers it has some explicit type arguments. Therefore, it reports that the
                 * only function matching the signature is A, because B does not have type parameters. However, actually B also matches the
                 * signature, and we must avoid dropping a.b.c from the call expression.
                 */
                if (callExpression?.typeArguments?.isNotEmpty() == true) typeArguments.addAll(functionCall.typeArguments)

                argumentList = functionCall.argumentList
                calleeReference = fakeCalleeReference
            }
        }
        val candidates = AllCandidatesResolver(shorteningContext.analysisSession.useSiteSession).getAllCandidates(
            firResolveSession, fakeFirQualifiedAccess, name, expressionInScope
        )
        return candidates.filter { overloadCandidate ->
            overloadCandidate.candidate.currentApplicability == CandidateApplicability.RESOLVED
        }
    }

    private fun findCandidatesForPropertyAccess(
        annotations: List<FirAnnotation>,
        typeArguments: List<FirTypeProjection>,
        name: Name,
        elementInScope: KtElement,
    ): List<OverloadCandidate> {
        val fakeCalleeReference = buildSimpleNamedReference { this.name = name }
        val fakeFirQualifiedAccess = buildPropertyAccessExpression {
            this.annotations.addAll(annotations)
            this.typeArguments.addAll(typeArguments)
            calleeReference = fakeCalleeReference
        }
        val candidates = AllCandidatesResolver(shorteningContext.analysisSession.useSiteSession).getAllCandidates(
            firResolveSession, fakeFirQualifiedAccess, name, elementInScope
        )
        return candidates.filter { overloadCandidate ->
            overloadCandidate.candidate.currentApplicability == CandidateApplicability.RESOLVED
        }
    }

    private fun List<OverloadCandidate>.findScopeForSymbol(symbol: FirBasedSymbol<*>): FirScope? = firstOrNull {
        it.candidate.symbol == symbol
    }?.candidate?.originScope

    /**
     * Returns whether a member of companion is used to initialize the enum entry or not. For example,
     *     enum class C(val i: Int) {
     *         ONE(<expr>C.K</expr>)  // C.ONE uses C.K for initialization
     *         ;
     *         companion object {
     *             const val K = 1
     *         }
     *     }
     */
    private fun KtExpression.isCompanionMemberUsedForEnumEntryInit(resolvedSymbol: FirCallableSymbol<*>): Boolean {
        val enumEntry = getNonStrictParentOfType<KtEnumEntry>() ?: return false
        val firEnumEntry = enumEntry.resolveToFirSymbol(firResolveSession) as? FirEnumEntrySymbol ?: return false
        val classNameOfResolvedSymbol = resolvedSymbol.callableId.className ?: return false
        return firEnumEntry.callableId.className == classNameOfResolvedSymbol.parent() &&
                classNameOfResolvedSymbol.shortName() == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
    }

    /**
     * Returns whether it is fine to shorten [firQualifiedAccess] or not.
     *
     * @param firQualifiedAccess FIR for the shortening target expression
     * @param calledSymbol The symbol referenced by the qualified access expression
     * @param expressionInScope An expression under the same scope as the shortening target expression
     *
     * The decision has two steps:
     *  1. Collect all candidates matching [firQualifiedAccess]
     *  - We use `AllCandidatesResolver(shorteningContext.analysisSession.useSiteSession).getAllCandidates( .. fake FIR .. )`. See
     *  [resolveUnqualifiedAccess] above.
     *  2. Check whether the candidate with the highest priority based on the distance to the scope from [expressionInScope] is the same
     *  as [calledSymbol] ot not
     *  - We use [hasScopeCloserThan] to determine the distance to the scope
     */
    private fun shortenIfAlreadyImported(
        firQualifiedAccess: FirQualifiedAccessExpression,
        calledSymbol: FirCallableSymbol<*>,
        expressionInScope: KtExpression,
    ): Boolean {
        /**
         * Avoid shortening reference to enum companion used in enum entry initialization there is no guarantee that the companion object
         * was initialized in advance.
         * For example, When we shorten the following code:
         *     enum class C(val i: Int) {
         *         ONE(<expr>C.K</expr>)  // shorten C.K to K
         *         ;
         *         companion object {
         *             const val K = 1
         *         }
         *     }
         *
         * the compiler reports "Variable 'K' must be initialized". This happens because there is no guarantee that the companion object
         * was initialized at the time when we use `C.K` for the enum entry `ONE`. To avoid this type of compiler errors, we don't shorten
         * the reference if it is a part of the enum companion object, and it is used by the enum entry initialization.
         */
        if (expressionInScope.isCompanionMemberUsedForEnumEntryInit(calledSymbol)) return false

        val candidates = resolveUnqualifiedAccess(firQualifiedAccess, calledSymbol.name, expressionInScope)

        val scopeForQualifiedAccess = candidates.findScopeForSymbol(calledSymbol) ?: return false
        if (candidates.mapNotNull { it.candidate.originScope }
                .hasScopeCloserThan(scopeForQualifiedAccess, expressionInScope)) return false
        val candidatesWithinSamePriorityScopes = candidates.filter { it.candidate.originScope == scopeForQualifiedAccess }
        if (candidatesWithinSamePriorityScopes.isEmpty() || candidatesWithinSamePriorityScopes.size == 1) return true

        /**
         * This is a conservative decision to avoid false positives.
         *
         * TODO: Figure out the priorities among `candidatesWithinSamePriorityScopes` and determine if [firQualifiedAccess] matches the
         * one with the highest priority. At this moment, we have some counter examples that [OverloadCandidate.isInBestCandidates] is true
         * and its symbol matches [firQualifiedAccess], but we cannot shorten it.
         *
         * For example:
         *   package foo
         *   class Foo {
         *       fun test() {
         *           // It references FIRST. Removing `foo` lets it reference SECOND. However, the one has true for
         *           // [OverloadCandidate.isInBestCandidates] is FIRST. Therefore, making a decision based on `isInBestCandidates` can
         *           // cause false positives i.e., shortening changes the referenced symbol.
         *           <caret>foo.myRun {
         *               42
         *           }
         *       }
         *   }
         *   inline fun <R> myRun(block: () -> R): R = block()         // FIRST
         *   inline fun <T, R> T.myRun(block: T.() -> R): R = block()  // SECOND
         */
        return false
    }

    private fun processPropertyReference(resolvedNamedReference: FirResolvedNamedReference) {
        val referenceExpression = resolvedNamedReference.psi as? KtNameReferenceExpression ?: return
        if (!referenceExpression.textRange.intersects(selection)) return
        val qualifiedProperty = referenceExpression.getDotQualifiedExpressionForSelector() ?: return

        val callableSymbol = resolvedNamedReference.resolvedSymbol as? FirCallableSymbol<*> ?: return

        val option = callableShortenOption(callableSymbol)
        if (option == ShortenOption.DO_NOT_SHORTEN) return

        val scopes = shorteningContext.findScopesAtPosition(qualifiedProperty, namesToImport, towerContextProvider) ?: return
        val availableCallables = shorteningContext.findPropertiesInScopes(scopes, callableSymbol.name)

        val firPropertyAccess = qualifiedProperty.getOrBuildFir(firResolveSession) as? FirQualifiedAccessExpression ?: return
        if (availableCallables.isNotEmpty() && shortenIfAlreadyImported(firPropertyAccess, callableSymbol, referenceExpression)) {
            addElementToShorten(ShortenQualifier(qualifiedProperty))
            return
        }
        if (option == ShortenOption.SHORTEN_IF_ALREADY_IMPORTED) return

        processCallableQualifiedAccess(
            callableSymbol,
            option,
            qualifiedProperty,
            availableCallables,
        )
    }

    private fun processFunctionCall(functionCall: FirFunctionCall) {
        if (!canBePossibleToDropReceiver(functionCall)) return

        val qualifiedCallExpression = functionCall.psi as? KtDotQualifiedExpression ?: return
        if (!qualifiedCallExpression.textRange.intersects(selection)) return
        val callExpression = qualifiedCallExpression.selectorExpression as? KtCallExpression ?: return

        val calleeReference = functionCall.calleeReference
        val calledSymbol = findUnambiguousReferencedCallableId(calleeReference) ?: return

        val option = callableShortenOption(calledSymbol)
        if (option == ShortenOption.DO_NOT_SHORTEN) return

        val scopes = shorteningContext.findScopesAtPosition(callExpression, namesToImport, towerContextProvider) ?: return
        val availableCallables = shorteningContext.findFunctionsInScopes(scopes, calledSymbol.name)
        if (availableCallables.isNotEmpty() && shortenIfAlreadyImported(functionCall, calledSymbol, callExpression)) {
            addElementToShorten(ShortenQualifier(qualifiedCallExpression))
            return
        }
        if (option == ShortenOption.SHORTEN_IF_ALREADY_IMPORTED) return

        processCallableQualifiedAccess(
            calledSymbol,
            option,
            qualifiedCallExpression,
            availableCallables,
        )
    }

    private fun processCallableQualifiedAccess(
        calledSymbol: FirCallableSymbol<*>,
        option: ShortenOption,
        qualifiedCallExpression: KtDotQualifiedExpression,
        availableCallables: List<AvailableSymbol<FirCallableSymbol<*>>>,
    ) {
        if (option == ShortenOption.DO_NOT_SHORTEN) return

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
            is FirErrorNamedReference -> getSingleUnambiguousCandidate(namedReference)
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
    private val targetFile: SmartPsiElementPointer<KtFile>,
    private val importsToAdd: List<FqName>,
    private val starImportsToAdd: List<FqName>,
    private val typesToShorten: List<SmartPsiElementPointer<KtUserType>>,
    private val qualifiersToShorten: List<SmartPsiElementPointer<KtDotQualifiedExpression>>,
) : ShortenCommand {

    override fun invokeShortening() {
        // if the file has been invalidated, there's nothing we can shorten
        val targetFile = targetFile.element ?: return

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

    override fun getTypesToShorten(): List<SmartPsiElementPointer<KtUserType>> = typesToShorten

    override fun getQualifiersToShorten(): List<SmartPsiElementPointer<KtDotQualifiedExpression>> = qualifiersToShorten
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
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.ElementsToShortenCollector.PartialOrderOfScope.Companion.toPartialOrder
import org.jetbrains.kotlin.analysis.api.fir.isImplicitDispatchReceiver
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver
import org.jetbrains.kotlin.analysis.api.fir.utils.computeImportableName
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.getAvailableScopesForPosition
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.AllCandidatesResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.OverloadCandidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnmatchedTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.PackageResolutionResult
import org.jetbrains.kotlin.fir.resolve.transformers.resolveToPackageOrClass
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirReferenceShortener(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaReferenceShortener, KaFirSessionComponent {
    private val context by lazy { FirShorteningContext(analysisSession) }

    override fun collectPossibleReferenceShorteningsInElement(
        element: KtElement,
        shortenOptions: ShortenOptions,
        classShortenStrategy: (KaClassLikeSymbol) -> ShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy
    ): ShortenCommand = withValidityAssertion {
        collectPossibleReferenceShortenings(
            element.containingKtFile,
            element.textRange,
            shortenOptions,
            classShortenStrategy,
            callableShortenStrategy
        )
    }

    override fun collectPossibleReferenceShortenings(
        file: KtFile,
        selection: TextRange,
        shortenOptions: ShortenOptions,
        classShortenStrategy: (KaClassLikeSymbol) -> ShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy
    ): ShortenCommand = withValidityAssertion {
        require(!file.isCompiled) { "No sense to collect references for shortening in compiled file $file" }

        val declarationToVisit = file.findSmallestElementOfTypeContainingSelection<KtDeclaration>(selection)
            ?: file

        val firDeclaration = declarationToVisit.getCorrespondingFirElement() ?: return ShortenCommandImpl(
            @Suppress("DEPRECATION")
            file.createSmartPointer(),
            importsToAdd = emptySet(),
            starImportsToAdd = emptySet(),
            listOfTypeToShortenInfo = emptyList(),
            listOfQualifierToShortenInfo = emptyList(),
            thisLabelsToShorten = emptyList(),
            kDocQualifiersToShorten = emptyList(),
        )

        val towerContext = FirTowerDataContextProvider.create(firResolveSession, declarationToVisit)

        //TODO: collect all usages of available symbols in the file and prevent importing symbols that could introduce name clashes, which
        // may alter the meaning of existing code.
        val collector = ElementsToShortenCollector(
            shortenOptions,
            context,
            towerContext,
            file,
            selection,
            classShortenStrategy = { classShortenStrategy(buildSymbol(it) as KaClassLikeSymbol) },
            callableShortenStrategy = { callableShortenStrategy(buildSymbol(it) as KaCallableSymbol) },
            firResolveSession,
        )
        firDeclaration.accept(CollectingVisitor(collector))

        val additionalImports = AdditionalImports(
            collector.getNamesToImport(starImport = false).toSet(),
            collector.getNamesToImport(starImport = true).toSet(),
        )

        val kDocCollector = KDocQualifiersToShortenCollector(
            analysisSession,
            selection,
            additionalImports,
            classShortenStrategy = {
                minOf(classShortenStrategy(buildSymbol(it) as KaClassLikeSymbol), ShortenStrategy.SHORTEN_IF_ALREADY_IMPORTED)
            },
            callableShortenStrategy = {
                minOf(callableShortenStrategy(buildSymbol(it) as KaCallableSymbol), ShortenStrategy.SHORTEN_IF_ALREADY_IMPORTED)
            },
        )
        kDocCollector.visitElement(declarationToVisit)

        @Suppress("DEPRECATION")
        return ShortenCommandImpl(
            file.createSmartPointer(),
            additionalImports.simpleImports,
            additionalImports.starImports,
            collector.typesToShorten.distinctBy { it.element }.map { TypeToShortenInfo(it.element.createSmartPointer(), it.shortenedRef) },
            collector.qualifiersToShorten.distinctBy { it.element }.map { QualifierToShortenInfo(it.element.createSmartPointer(), it.shortenedRef) },
            collector.labelsToShorten.distinctBy { it.element }.map { ThisLabelToShortenInfo(it.element.createSmartPointer()) },
            kDocCollector.kDocQualifiersToShorten.distinctBy { it.element }.map { it.element.createSmartPointer() },
        )
    }

    private fun KtElement.getCorrespondingFirElement(): FirElement? {
        require(this is KtFile || this is KtDeclaration)

        val firElement = getOrBuildFir(firResolveSession)

        return when (firElement) {
            is FirDeclaration -> firElement
            is FirAnonymousFunctionExpression -> firElement.anonymousFunction
            is FirFunctionTypeParameter -> firElement
            else -> null
        }
    }

    private fun buildSymbol(firSymbol: FirBasedSymbol<*>): KaSymbol = analysisSession.firSymbolBuilder.buildSymbol(firSymbol)
}

private class FirTowerDataContextProvider private constructor(
    private val contextProvider: ContextCollector.ContextProvider
) {
    companion object {
        fun create(firResolveSession: LLFirResolveSession, targetElement: KtElement): FirTowerDataContextProvider {
            val firFile = targetElement.containingKtFile.getOrBuildFirFile(firResolveSession)

            val sessionHolder = run {
                val firSession = firResolveSession.useSiteFirSession
                val scopeSession = firResolveSession.getScopeSessionFor(firSession)

                SessionHolderImpl(firSession, scopeSession)
            }

            val designation = ContextCollector.computeDesignation(firFile, targetElement)

            val contextProvider = ContextCollector.process(
                firFile,
                sessionHolder,
                designation,
                shouldCollectBodyContext = false, // we only query SELF context
                filter = { ContextCollector.FilterResponse.CONTINUE }
            )

            return FirTowerDataContextProvider(contextProvider)
        }
    }

    fun getClosestAvailableParentContext(ktElement: KtElement): FirTowerDataContext? {
        for (parent in ktElement.parentsWithSelf) {
            val context = contextProvider[parent, ContextCollector.ContextKind.SELF]

            if (context != null) {
                return context.towerDataContext
            }
        }

        return null
    }
}

private fun FqName.dropFakeRootPrefixIfPresent(): FqName =
    tail(FqName(ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE))

private data class AdditionalImports(val simpleImports: Set<FqName>, val starImports: Set<FqName>)

private inline fun <reified T : KtElement> KtFile.findSmallestElementOfTypeContainingSelection(selection: TextRange): T? =
    findElementAt(selection.startOffset)
        ?.parentsOfType<T>(withSelf = true)
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

    /** Implicitly imported from package. */
    PACKAGE,

    /** Explicitly imported by Kotlin default. For example, `kotlin.String`. */
    DEFAULT_EXPLICIT,

    /** Star imported (star import) by user. */
    STAR,

    /** Star imported (star import) by Kotlin default. */
    DEFAULT_STAR;

    fun hasHigherPriorityThan(that: ImportKind): Boolean = this < that

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

        fun fromShortenOption(option: ShortenStrategy): ImportKind? = when (option) {
            ShortenStrategy.SHORTEN_AND_IMPORT -> EXPLICIT
            ShortenStrategy.SHORTEN_AND_STAR_IMPORT -> STAR
            else -> null
        }
    }
}

private data class AvailableSymbol<out T>(
    val symbol: T,
    val importKind: ImportKind,
)

private class FirShorteningContext(val analysisSession: KaFirSession) {
    private val firResolveSession = analysisSession.firResolveSession

    private val firSession: FirSession
        get() = firResolveSession.useSiteFirSession

    class ClassifierCandidate(val scope: FirScope, val availableSymbol: AvailableSymbol<FirClassifierSymbol<*>>)

    fun findFirstClassifierInScopesByName(positionScopes: List<FirScope>, targetClassName: Name): AvailableSymbol<FirClassifierSymbol<*>>? =
        positionScopes.firstNotNullOfOrNull { scope -> findFirstClassifierSymbolByName(scope, targetClassName) }

    fun findClassifiersInScopesByName(scopes: List<FirScope>, targetClassName: Name): List<ClassifierCandidate> =
        scopes.mapNotNull { scope ->
            val classifierSymbol = findFirstClassifierSymbolByName(scope, targetClassName) ?: return@mapNotNull null

            ClassifierCandidate(scope, classifierSymbol)
        }

    private fun findFirstClassifierSymbolByName(scope: FirScope, targetClassName: Name): AvailableSymbol<FirClassifierSymbol<*>>? {
        val classifierSymbol = scope.findFirstClassifierByName(targetClassName) ?: return null

        return AvailableSymbol(classifierSymbol, ImportKind.fromScope(scope))
    }

    private fun FirClassLikeSymbol<*>.getSamConstructor(): FirNamedFunctionSymbol? {
        val samResolver = FirSamResolver(firSession, analysisSession.getScopeSessionFor(firSession))

        return samResolver.getSamConstructor(fir)?.symbol
    }

    /**
     * Finds constructors with a given [targetClassName] available within the [scope], including SAM constructors
     * (which are not explicitly declared in the class).
     *
     * Includes type-aliased constructors too if typealias confirms to the [targetClassName].
     *
     * Do not confuse with constructors **declared** in the scope (see [FirScope.processDeclaredConstructors]).
     */
    private fun findAvailableConstructors(scope: FirScope, targetClassName: Name): List<FirFunctionSymbol<*>> {
        val classLikeSymbol = scope.findFirstClassifierByName(targetClassName) as? FirClassLikeSymbol
            ?: return emptyList()

        @OptIn(DirectDeclarationsAccess::class)
        val constructors = (classLikeSymbol as? FirClassSymbol)?.declarationSymbols?.filterIsInstance<FirConstructorSymbol>().orEmpty()
        val samConstructor = classLikeSymbol.getSamConstructor()

        return constructors + listOfNotNull(samConstructor)
    }

    fun findFunctionsInScopes(scopes: List<FirScope>, name: Name): List<AvailableSymbol<FirFunctionSymbol<*>>> {
        return scopes.flatMap { scope ->
            val importKind = ImportKind.fromScope(scope)
            buildList {
                // Collect constructors
                findAvailableConstructors(scope, name).mapTo(this) { AvailableSymbol(it, importKind) }

                // Collect functions
                scope.getFunctions(name).mapTo(this) { AvailableSymbol(it, importKind) }
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
        newImports: Sequence<FqName>,
        towerContextProvider: FirTowerDataContextProvider,
        withImplicitReceivers: Boolean = true,
    ): List<FirScope>? {
        val towerDataContext = towerContextProvider.getClosestAvailableParentContext(position) ?: return null
        val nonLocalScopes = towerDataContext.nonLocalTowerDataElements
            .asSequence()
            .filter { withImplicitReceivers || it.implicitReceiver == null }
            .flatMap {
                // We must use `it.getAvailableScopesForPosition(position)` instead of `it.scope` to check scopes of companion objects
                // and context receivers as well.
                it.getAvailableScopesForPosition(position)
            }

        val result = buildList {
            addAll(nonLocalScopes)
            addIfNotNull(createFakeImportingScope(newImports))
            addAll(towerDataContext.localScopes)
        }

        return result.asReversed()
    }

    private fun createFakeImportingScope(newImports: Sequence<FqName>): FirScope? {
        val resolvedNewImports = newImports.mapNotNull { createFakeResolvedImport(it) }.toList()
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

    fun getRegularClass(type: ConeKotlinType?): FirRegularClass? {
        return type?.toRegularClassSymbol(firSession)?.fir
    }

    fun toClassSymbol(classId: ClassId) =
        firSession.symbolProvider.getClassLikeSymbolByClassId(classId)

    fun convertToImportableName(callableSymbol: FirCallableSymbol<*>): FqName? =
        callableSymbol.computeImportableName()
}

private sealed class ElementToShorten {
    abstract val element: KtElement
    abstract val nameToImport: FqName?
    abstract val importAllInParent: Boolean
}

private class ShortenType(
    override val element: KtUserType,
    val shortenedRef: String? = null,
    override val nameToImport: FqName? = null,
    override val importAllInParent: Boolean = false,
) : ElementToShorten()

private class ShortenQualifier(
    override val element: KtDotQualifiedExpression,
    val shortenedRef: String? = null,
    override val nameToImport: FqName? = null,
    override val importAllInParent: Boolean = false
) : ElementToShorten()

private class ShortenThisLabel(
    override val element: KtThisExpression,
) : ElementToShorten() {
    override val nameToImport: FqName? = null
    override val importAllInParent: Boolean = false
}

/**
 * N.B. Does not subclass [ElementToShorten] because currently
 * there's no reason to do that.
 */
private class ShortenKDocQualifier(
    val element: KDocName,
)

private class CollectingVisitor(private val collector: ElementsToShortenCollector) : FirVisitorVoid() {
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

    override fun visitScript(script: FirScript) {
        @OptIn(DirectDeclarationsAccess::class)
        script.declarations.forEach {
            it.accept(this)
        }
    }

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
        visitResolvedTypeRef(errorTypeRef)

        errorTypeRef.partiallyResolvedTypeRef?.accept(this)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        collector.processTypeRef(resolvedTypeRef)

        resolvedTypeRef.acceptChildren(this)
        resolvedTypeRef.delegatedTypeRef?.accept(this)
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        super.visitResolvedQualifier(resolvedQualifier)

        collector.processTypeQualifier(resolvedQualifier)
    }

    override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier) {
        super.visitErrorResolvedQualifier(errorResolvedQualifier)

        collector.processTypeQualifier(errorResolvedQualifier)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        super.visitFunctionCall(functionCall)

        collector.processFunctionCall(functionCall)
    }

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
        super.visitPropertyAccessExpression(propertyAccessExpression)

        collector.processPropertyAccess(propertyAccessExpression)
    }

    override fun visitThisReference(thisReference: FirThisReference) {
        super.visitThisReference(thisReference)

        collector.processThisReference(thisReference)
    }
}

private class ElementsToShortenCollector(
    private val shortenOptions: ShortenOptions,
    private val shorteningContext: FirShorteningContext,
    private val towerContextProvider: FirTowerDataContextProvider,
    private val containingFile: KtFile,
    private val selection: TextRange,
    private val classShortenStrategy: (FirClassLikeSymbol<*>) -> ShortenStrategy,
    private val callableShortenStrategy: (FirCallableSymbol<*>) -> ShortenStrategy,
    private val firResolveSession: LLFirResolveSession,
) {
    val typesToShorten: MutableList<ShortenType> = mutableListOf()
    val qualifiersToShorten: MutableList<ShortenQualifier> = mutableListOf()
    val labelsToShorten: MutableList<ShortenThisLabel> = mutableListOf()

    fun processTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        val typeElement = resolvedTypeRef.correspondingTypePsi ?: return
        if (typeElement.qualifier == null) return

        val classifierId = resolvedTypeRef.coneType.abbreviatedTypeOrSelf.lowerBoundIfFlexible().candidateClassId ?: return

        findClassifierQualifierToShorten(classifierId, typeElement)?.let(::addElementToShorten)
    }

    /**
     * Retrieves the corresponding [KtUserType] PSI the given [FirResolvedTypeRef].
     *
     * This code handles some quirks of FIR sources and PSI:
     * - in `vararg args: String` declaration, `String` type reference has fake source, but `Array<String>` has real source
     * (see [KtFakeSourceElementKind.ArrayTypeFromVarargParameter]).
     * - if FIR reference points to the type with generic parameters (like `Foo<Bar>`), its source is not [KtTypeReference], but
     * [KtNameReferenceExpression].
     */
    private val FirResolvedTypeRef.correspondingTypePsi: KtUserType?
        get() {
            val sourcePsi = when {
                // array type for vararg parameters is not present in the code, so no need to handle it
                delegatedTypeRef?.source?.kind == KtFakeSourceElementKind.ArrayTypeFromVarargParameter -> null

                // but the array's underlying type is present with a fake source, and needs to be handled
                source?.kind == KtFakeSourceElementKind.ArrayTypeFromVarargParameter -> psi

                else -> realPsi
            }

            val outerTypeElement = when (sourcePsi) {
                is KtTypeReference -> sourcePsi.typeElement
                is KtNameReferenceExpression -> sourcePsi.parent as? KtTypeElement
                else -> null
            }

            return outerTypeElement?.unwrapNullability() as? KtUserType
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

    fun getNamesToImport(starImport: Boolean = false): Sequence<FqName> = sequence {
        yieldAll(typesToShorten)
        yieldAll(qualifiersToShorten)
    }.filter { starImport == it.importAllInParent }.mapNotNull { it.nameToImport }.distinct()

    private fun findFakePackageToShorten(typeElement: KtUserType): ElementToShorten? {
        val deepestTypeWithQualifier = typeElement.qualifiedTypesWithSelf.last()

        return if (deepestTypeWithQualifier.hasFakeRootPrefix()) createElementToShorten(deepestTypeWithQualifier) else null
    }

    fun processTypeQualifier(resolvedQualifier: FirResolvedQualifier) {
        if (resolvedQualifier.isImplicitDispatchReceiver) return

        val wholeClassQualifier = resolvedQualifier.classId ?: return
        val qualifierPsi = resolvedQualifier.psi ?: return

        val wholeQualifierElement = when (qualifierPsi) {
            is KtDotQualifiedExpression -> qualifierPsi
            is KtNameReferenceExpression -> qualifierPsi.getDotQualifiedExpressionForSelector() ?: return
            else -> return
        }

        findClassifierQualifierToShorten(wholeClassQualifier, wholeQualifierElement)?.let(::addElementToShorten)
    }

    private val FirClassifierSymbol<*>.classIdIfExists: ClassId?
        get() = (this as? FirClassLikeSymbol<*>)?.classId

    private val FirConstructorSymbol.classIdIfExists: ClassId?
        get() = this.containingClassLookupTag()?.classId

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
        is FirNestedClassifierScopeWithSubstitution -> originalScope.correspondingClassIdIfExists()
        is FirClassUseSiteMemberScope -> ownerClassLookupTag.classId
        else -> errorWithAttachment("FirScope ${this::class}` is expected to be one of FirNestedClassifierScope and FirClassUseSiteMemberScope to get ClassId") {
            withEntry("firScope", this@correspondingClassIdIfExists) { it.toString() }
        }
    }

    private fun ClassId.idWithoutCompanion() = if (shortClassName == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) outerClassId else this

    private fun FirScope.isScopeForClass(): Boolean = when {
        this is FirNestedClassifierScope -> true
        this is FirNestedClassifierScopeWithSubstitution -> originalScope.isScopeForClass()
        this is FirClassUseSiteMemberScope -> true
        else -> false
    }

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
    private fun FirScope.isScopeForClassCloserThanAnotherScopeForClass(another: FirScope, from: KtClassOrObject): Boolean {
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
    private fun findMostInnerClassMatchingId(innerClass: KtClassOrObject, candidates: Set<ClassId>): ClassId? {
        var classInNestedClass: KtClassOrObject? = innerClass
        while (classInNestedClass != null) {
            val containingClassId = classInNestedClass.getClassId()
            if (containingClassId in candidates) return containingClassId
            classInNestedClass = classInNestedClass.findClassOrObjectParent()
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
        ScriptDeclarations(2),
        ClassUseSite(2),
        NestedClassifier(2),
        TypeParameter(2),
        ExplicitSimpleImporting(3),
        PackageMember(4),
        Unclassified(5),
        ;

        companion object {
            fun FirScope.toPartialOrder(): PartialOrderOfScope {
                return when (this) {
                    is FirLocalScope -> Local
                    is FirScriptDeclarationsScope -> ScriptDeclarations
                    is FirClassUseSiteMemberScope -> ClassUseSite
                    is FirNestedClassifierScope -> NestedClassifier
                    is FirTypeParameterScope -> TypeParameter
                    is FirNestedClassifierScopeWithSubstitution -> originalScope.toPartialOrder()
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
            val classContainingFrom = from.findClassOrObjectParent() ?: return@any false
            return@any scope.isScopeForClassCloserThanAnotherScopeForClass(base, classContainingFrom)
        }
        base.isWiderThan(scope)
    }

    /**
     * Returns true if [containingFile] has a [KtImportDirective] whose imported FqName is the same as [classId] but references a different
     * symbol.
     */
    private fun importDirectiveForDifferentSymbolWithSameNameIsPresent(classId: ClassId): Boolean {
        val importDirectivesWithSameImportedFqName = containingFile.collectDescendantsOfType { importedDirective: KtImportDirective ->
            importedDirective.importedFqName?.shortName() == classId.shortClassName
        }
        return importDirectivesWithSameImportedFqName.isNotEmpty() &&
                importDirectivesWithSameImportedFqName.all { it.importedFqName != classId.asSingleFqName() }
    }

    private fun shortenClassifierIfAlreadyImported(
        classId: ClassId,
        element: KtElement,
        classSymbol: FirClassLikeSymbol<*>,
        scopes: List<FirScope>,
    ): Boolean {
        val name = classId.shortClassName
        val availableClassifiers = shorteningContext.findClassifiersInScopesByName(scopes, name)
        val matchingAvailableSymbol = availableClassifiers.firstOrNull { it.availableSymbol.symbol.classIdIfExists == classId }
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
        if (shorteningContext.findPropertiesInScopes(scopes, name).isNotEmpty()) {
            val firForElement = element.getOrBuildFir(firResolveSession) as? FirQualifiedAccessExpression
            val typeArguments = firForElement?.typeArguments ?: emptyList()
            val qualifiedAccessCandidates = findCandidatesForPropertyAccess(classSymbol.annotations, typeArguments, name, element)
            if (qualifiedAccessCandidates.mapNotNull { it.candidate.originScope }.hasScopeCloserThan(scopeForClass, element)) return false
        }

        return !importDirectiveForDifferentSymbolWithSameNameIsPresent(classId)
    }

    private fun shortenIfAlreadyImportedAsAlias(referenceExpression: KtElement, referencedSymbolFqName: FqName): ElementToShorten? {
        val importDirectiveForReferencedSymbol = containingFile.importDirectives.firstOrNull {
            it.importedFqName == referencedSymbolFqName && it.alias != null
        } ?: return null

        val aliasedName = importDirectiveForReferencedSymbol.alias?.name
        return createElementToShorten(referenceExpression, shortenedRef = aliasedName)
    }

    private fun shortenClassifierQualifier(
        positionScopes: List<FirScope>,
        qualifierClassId: ClassId,
        qualifierElement: KtElement,
    ): ElementToShorten? {
        val classSymbol = shorteningContext.toClassSymbol(qualifierClassId) ?: return null

        val option = classShortenStrategy(classSymbol)
        if (option == ShortenStrategy.DO_NOT_SHORTEN) return null

        // If its parent has a type parameter, we do not shorten it ATM because it will lose its type parameter. See KTIJ-26072
        if (classSymbol.hasTypeParameterFromParent()) return null

        shortenIfAlreadyImportedAsAlias(qualifierElement, qualifierClassId.asSingleFqName())?.let { return it }

        if (shortenClassifierIfAlreadyImported(qualifierClassId, qualifierElement, classSymbol, positionScopes)) {
            return createElementToShorten(qualifierElement)
        }
        if (option == ShortenStrategy.SHORTEN_IF_ALREADY_IMPORTED) return null

        val importAllInParent = option == ShortenStrategy.SHORTEN_AND_STAR_IMPORT
        if (importBreaksExistingReferences(qualifierClassId, importAllInParent)) return null

        // Find class with the same name that's already available in this file.
        val availableClassifier = shorteningContext.findFirstClassifierInScopesByName(positionScopes, qualifierClassId.shortClassName)

        when {
            // No class with name `classId.shortClassName` is present in the scope. Hence, we can safely import the name and shorten
            // the reference.
            availableClassifier == null -> {
                return createElementToShorten(
                    qualifierElement,
                    qualifierClassId.asSingleFqName(),
                    importAllInParent
                )
            }
            // The class with name `classId.shortClassName` happens to be the same class referenced by this qualified access.
            availableClassifier.symbol.classIdIfExists == qualifierClassId -> {
                // Respect caller's request to use star import, if it's not already star-imported.
                return when {
                    availableClassifier.importKind == ImportKind.EXPLICIT && importAllInParent -> {
                        createElementToShorten(qualifierElement, qualifierClassId.asSingleFqName(), importAllInParent)
                    }
                    // Otherwise, just shorten it and don't alter import statements
                    else -> createElementToShorten(qualifierElement)
                }
            }
            importedClassifierOverwritesAvailableClassifier(availableClassifier, importAllInParent) -> {
                return createElementToShorten(qualifierElement, qualifierClassId.asSingleFqName(), importAllInParent)
            }
        }

        return null
    }

    /**
     * Finds the longest qualifier in [wholeQualifierElement] which can be safely shortened in the [positionScopes].
     * [wholeQualifierClassId] is supposed to reflect the class which is referenced by the [wholeQualifierElement].
     *
     * N.B. Even if the [wholeQualifierElement] is not strictly in the [selection],
     * some outer part of it might be, and we want to shorten that.
     * So we have to check all the outer qualifiers.
     */
    private fun findClassifierQualifierToShorten(
        wholeQualifierClassId: ClassId,
        wholeQualifierElement: KtElement,
    ): ElementToShorten? {
        val positionScopes = shorteningContext.findScopesAtPosition(
            wholeQualifierElement,
            getNamesToImport(),
            towerContextProvider,
            withImplicitReceivers = false,
        ) ?: return null

        val allClassIds = wholeQualifierClassId.outerClassesWithSelf
        val allQualifiedElements = wholeQualifierElement.qualifiedElementsWithSelf

        for ((classId, element) in allClassIds.zip(allQualifiedElements)) {
            if (!element.inSelection) continue

            shortenClassifierQualifier(positionScopes, classId, element)?.let { return it }
        }

        val lastQualifier = allQualifiedElements.last()
        if (!lastQualifier.inSelection) return null

        return findFakePackageToShorten(lastQualifier)
    }

    private fun createElementToShorten(
        element: KtElement,
        referencedSymbol: FqName? = null,
        importAllInParent: Boolean = false,
        shortenedRef: String? = null,
    ): ElementToShorten {
        var nameToImport = if (importAllInParent) {
            referencedSymbol?.parentOrNull() ?: error("Provided FqName '$referencedSymbol' cannot be imported with a star")
        } else {
            referencedSymbol
        }

        return when (element) {
            is KtUserType -> ShortenType(element, shortenedRef, nameToImport, importAllInParent)
            is KtDotQualifiedExpression -> ShortenQualifier(element, shortenedRef, nameToImport, importAllInParent)
            is KtThisExpression -> ShortenThisLabel(element)
            else -> error("Unexpected ${element::class}")
        }
    }

    private fun findFakePackageToShorten(element: KtElement): ElementToShorten? {
        return when (element) {
            is KtUserType -> findFakePackageToShorten(element)
            is KtDotQualifiedExpression -> findFakePackageToShorten(element)
            else -> error("Unexpected ${element::class}")
        }
    }

    /**
     * Returns `true` if adding [classToImport] import to the [file] might alter or break the
     * resolve of existing references in the file.
     *
     * N.B.: At the moment it might have both false positives and false negatives, since it does not
     * check all possible references.
     */
    private fun importBreaksExistingReferences(classToImport: ClassId, importAllInParent: Boolean): Boolean {
        return importAffectsUsagesOfClassesWithSameName(classToImport, importAllInParent)
    }

    /**
     * Same as above, but for more general callable symbols.
     *
     * Currently only checks constructor calls, assuming `true` for everything else.
     */
    private fun importBreaksExistingReferences(callableToImport: FirCallableSymbol<*>, importAllInParent: Boolean): Boolean {
        if (callableToImport is FirConstructorSymbol) {
            val classToImport = callableToImport.classIdIfExists
            if (classToImport != null) {
                return importAffectsUsagesOfClassesWithSameName(classToImport, importAllInParent)
            }
        }

        return false
    }

    private fun importedClassifierOverwritesAvailableClassifier(
        availableClassifier: AvailableSymbol<FirClassifierSymbol<*>>,
        importAllInParent: Boolean
    ): Boolean {
        val importKindFromOption = if (importAllInParent) ImportKind.STAR else ImportKind.EXPLICIT
        return importKindFromOption.hasHigherPriorityThan(availableClassifier.importKind)
    }

    private fun importAffectsUsagesOfClassesWithSameName(classToImport: ClassId, importAllInParent: Boolean): Boolean {
        var importAffectsUsages = false

        containingFile.accept(object : KtVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitImportList(importList: KtImportList) {}

            override fun visitPackageDirective(directive: KtPackageDirective) {}

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                if (importAffectsUsages) return
                if (KtPsiUtil.isSelectorInQualified(expression)) return

                val shortClassName = classToImport.shortClassName
                if (expression.getReferencedNameAsName() != shortClassName) return

                val contextProvider = FirTowerDataContextProvider.create(firResolveSession, expression)
                val positionScopes = shorteningContext.findScopesAtPosition(expression, getNamesToImport(), contextProvider) ?: return
                val availableClassifier = shorteningContext.findFirstClassifierInScopesByName(positionScopes, shortClassName) ?: return
                when {
                    availableClassifier.symbol.classIdIfExists == classToImport -> return
                    importedClassifierOverwritesAvailableClassifier(availableClassifier, importAllInParent) -> {
                        importAffectsUsages = true
                    }
                }
            }
        })

        return importAffectsUsages
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
        val candidates = AllCandidatesResolver(shorteningContext.analysisSession.firSession).getAllCandidates(
            firResolveSession, fakeFirQualifiedAccess, name, expressionInScope, ResolutionMode.ContextIndependent,
        )
        return candidates.filter { overloadCandidate ->
            when (overloadCandidate.candidate.lowestApplicability) {
                CandidateApplicability.RESOLVED -> true
                CandidateApplicability.K2_SYNTHETIC_RESOLVED -> true // SAM constructor call
                else -> false
            }
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
        val candidates = AllCandidatesResolver(shorteningContext.analysisSession.firSession).getAllCandidates(
            firResolveSession, fakeFirQualifiedAccess, name, elementInScope, ResolutionMode.ContextIndependent,
        )
        return candidates.filter { overloadCandidate ->
            overloadCandidate.candidate.lowestApplicability == CandidateApplicability.RESOLVED
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

        if (candidatesWithinSamePriorityScopes.isEmpty()) {
            return true
        }

        val singleCandidate = candidatesWithinSamePriorityScopes.singleOrNull() ?: return false

        // TODO isInBestCandidates should probably be used more actively to filter candidates
        return singleCandidate.isInBestCandidates &&
                areReceiversEquivalent(firQualifiedAccess, singleCandidate.candidate)
    }

    fun processPropertyAccess(firPropertyAccess: FirPropertyAccessExpression) {
        // if explicit receiver is a property access or a function call, we cannot shorten it
        if (!canBePossibleToDropReceiver(firPropertyAccess)) return

        val propertyReferenceExpression = firPropertyAccess.correspondingNameReference ?: return
        val qualifiedProperty = propertyReferenceExpression.getQualifiedElement() as? KtDotQualifiedExpression ?: return

        if (!qualifiedProperty.inSelection) return

        val propertySymbol = firPropertyAccess.referencedSymbol ?: return

        val option = callableShortenStrategy(propertySymbol)
        if (option == ShortenStrategy.DO_NOT_SHORTEN) return

        shortenIfAlreadyImportedAsAlias(qualifiedProperty, propertySymbol.callableId.asSingleFqName())?.let {
            addElementToShorten(it)
            return
        }

        val scopes = shorteningContext.findScopesAtPosition(qualifiedProperty, getNamesToImport(), towerContextProvider) ?: return
        val availableCallables = shorteningContext.findPropertiesInScopes(scopes, propertySymbol.name)
        if (availableCallables.isNotEmpty() && shortenIfAlreadyImported(firPropertyAccess, propertySymbol, qualifiedProperty)) {
            addElementToShorten(createElementToShorten(qualifiedProperty))
            return
        }
        if (option == ShortenStrategy.SHORTEN_IF_ALREADY_IMPORTED) return

        findCallableQualifiedAccessToShorten(
            firPropertyAccess,
            propertySymbol,
            option,
            qualifiedProperty,
            availableCallables,
        )?.let(::addElementToShorten)
    }

    private val FirPropertyAccessExpression.correspondingNameReference: KtNameReferenceExpression?
        get() {
            val nameReference = when (val sourcePsi = psi) {
                // usual `foo.bar.baz` case
                is KtDotQualifiedExpression -> sourcePsi.selectorExpression

                // short `foo` case, or implicit invoke call like `foo.bar.baz()`
                else -> sourcePsi
            }

            return nameReference as? KtNameReferenceExpression
        }

    private val FirPropertyAccessExpression.referencedSymbol: FirVariableSymbol<*>?
        get() = (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirVariableSymbol<*>

    fun processFunctionCall(functionCall: FirFunctionCall) {
        if (!canBePossibleToDropReceiver(functionCall)) return

        val qualifiedCallExpression = functionCall.psi as? KtDotQualifiedExpression ?: return
        val callExpression = qualifiedCallExpression.selectorExpression as? KtCallExpression ?: return

        if (!qualifiedCallExpression.inSelection) return

        val calleeReference = functionCall.calleeReference
        val calledSymbol = findUnambiguousReferencedCallableId(calleeReference) ?: return

        val option = callableShortenStrategy(calledSymbol)
        if (option == ShortenStrategy.DO_NOT_SHORTEN) return

        shortenIfAlreadyImportedAsAlias(qualifiedCallExpression, calledSymbol.callableId.asSingleFqName())?.let {
            addElementToShorten(it)
            return
        }

        val scopes = shorteningContext.findScopesAtPosition(callExpression, getNamesToImport(), towerContextProvider) ?: return
        val availableCallables = shorteningContext.findFunctionsInScopes(scopes, calledSymbol.name)
        if (availableCallables.isNotEmpty() && shortenIfAlreadyImported(functionCall, calledSymbol, callExpression)) {
            addElementToShorten(createElementToShorten(qualifiedCallExpression))
            return
        }
        if (option == ShortenStrategy.SHORTEN_IF_ALREADY_IMPORTED) return

        findCallableQualifiedAccessToShorten(
            functionCall,
            calledSymbol,
            option,
            qualifiedCallExpression,
            availableCallables,
        )?.let(::addElementToShorten)
    }

    private fun findCallableQualifiedAccessToShorten(
        qualifiedAccess: FirQualifiedAccessExpression,
        calledSymbol: FirCallableSymbol<*>,
        option: ShortenStrategy,
        qualifiedCallExpression: KtDotQualifiedExpression,
        availableCallables: List<AvailableSymbol<FirCallableSymbol<*>>>,
    ): ElementToShorten? {
        if (option == ShortenStrategy.DO_NOT_SHORTEN) return null
        if (!canBePossibleToImportReceiver(qualifiedAccess)) return null

        val nameToImport = shorteningContext.convertToImportableName(calledSymbol)

        val (matchedCallables, otherCallables) = availableCallables.partition { it.symbol.callableId == calledSymbol.callableId }

        val importKindFromOption = ImportKind.fromShortenOption(option)
        val importKind = matchedCallables.minOfOrNull { it.importKind } ?: importKindFromOption ?: return null

        val callToShorten = when {
            otherCallables.all { importKind.hasHigherPriorityThan(it.importKind) } -> {
                when {
                    matchedCallables.isEmpty() -> {
                        if (nameToImport == null || option == ShortenStrategy.SHORTEN_IF_ALREADY_IMPORTED) return null

                        val importAllInParent = option == ShortenStrategy.SHORTEN_AND_STAR_IMPORT
                        if (importBreaksExistingReferences(calledSymbol, importAllInParent)) return null

                        createElementToShorten(
                            qualifiedCallExpression,
                            nameToImport,
                            importAllInParent,
                        )
                    }

                    // Respect caller's request to star import this symbol.
                    matchedCallables.any { it.importKind == ImportKind.EXPLICIT } && option == ShortenStrategy.SHORTEN_AND_STAR_IMPORT ->
                        createElementToShorten(qualifiedCallExpression, nameToImport, importAllInParent = true)

                    else -> createElementToShorten(qualifiedCallExpression)
                }
            }
            else -> findFakePackageToShorten(qualifiedCallExpression)
        }

        return callToShorten
    }

    private fun canBePossibleToDropReceiver(qualifiedAccess: FirQualifiedAccessExpression): Boolean {
        return when (val explicitReceiver = qualifiedAccess.explicitReceiver) {
            is FirThisReceiverExpression -> {
                // any non-implicit 'this' receiver can potentially be shortened by reference shortener
                shortenOptions.removeThis && !explicitReceiver.isImplicit
            }

            else -> canBePossibleToImportReceiver(qualifiedAccess)
        }
    }

    private fun canBePossibleToImportReceiver(firQualifiedAccess: FirQualifiedAccessExpression): Boolean {
        return firQualifiedAccess.explicitReceiver is FirResolvedQualifier &&
                firQualifiedAccess.extensionReceiver == null
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

        val distinctCandidates = candidates.distinctBy { candidate ->
            // A workaround to squash functions and constructors with the same name together.
            when (candidate) {
                is FirConstructorSymbol -> {
                    val classId = candidate.typeAliasConstructorInfo?.typeAliasSymbol?.classId ?: candidate.classIdIfExists
                    classId?.asSingleFqName()
                }
                else -> candidate.callableId.asSingleFqName()
            }
        }

        return distinctCandidates.singleOrNull()
    }

    private fun findFakePackageToShorten(wholeQualifiedExpression: KtDotQualifiedExpression): ElementToShorten? {
        val deepestQualifier = wholeQualifiedExpression.qualifiedExpressionsWithSelf.last()
        return if (deepestQualifier.hasFakeRootPrefix()) createElementToShorten(deepestQualifier) else null
    }

    /**
     * Checks whether `this` expression references the closest receiver in the current position.
     *
     * If it is the case, then we can safely remove the label from it (if it exists).
     */
    private fun FirThisReference.referencesClosestReceiver(): Boolean {
        require(!isImplicit) {
            "It doesn't make sense to handle implicit this references"
        }

        if (labelName == null) return true

        val psi = psi as? KtThisExpression ?: return false
        val implicitReceivers = towerContextProvider.getClosestAvailableParentContext(psi)?.implicitValueStorage ?: return false
        val closestImplicitReceiver = implicitReceivers.implicitReceivers.lastOrNull() ?: return false

        return boundSymbol == closestImplicitReceiver.boundSymbol
    }

    private fun canBePossibleToDropLabel(thisReference: FirThisReference): Boolean {
        return shortenOptions.removeThisLabels && thisReference.labelName != null
    }

    /**
     * This method intentionally mirrors the appearance
     * of the [classShortenStrategy] and [callableShortenStrategy] filters,
     * but ATM we don't have a way to properly handle
     * [FirThisReference]s through the existing filters.
     *
     * We need a better way to decide shortening strategy
     * for labeled and regular `this` expressions (KT-63555).
     */
    private fun thisLabelShortenStrategy(thisReference: FirThisReference): ShortenStrategy {
        val referencedSymbol = thisReference.referencedMemberSymbol

        val strategy = when (referencedSymbol) {
            is FirClassLikeSymbol<*> -> classShortenStrategy(referencedSymbol)
            is FirCallableSymbol<*> -> callableShortenStrategy(referencedSymbol)
            else -> ShortenStrategy.DO_NOT_SHORTEN
        }

        return strategy
    }

    fun processThisReference(thisReference: FirThisReference) {
        if (!canBePossibleToDropLabel(thisReference)) return

        val labeledThisPsi = thisReference.psi as? KtThisExpression ?: return
        if (!labeledThisPsi.inSelection) return

        if (thisLabelShortenStrategy(thisReference) == ShortenStrategy.DO_NOT_SHORTEN) return

        if (thisReference.referencesClosestReceiver()) {
            addElementToShorten(createElementToShorten(labeledThisPsi))
        }
    }

    private fun KtElement.isInsideOf(another: KtElement): Boolean = another.textRange.contains(textRange)

    /**
     * Remove entries from [typesToShorten] and [qualifiersToShorten] if their qualifiers will be shortened
     * when we shorten [qualifier].
     */
    private fun removeRedundantElements(qualifier: KtElement) {
        typesToShorten.removeAll { it.element.qualifier?.isInsideOf(qualifier) == true }
        qualifiersToShorten.removeAll { it.element.receiverExpression.isInsideOf(qualifier) }
    }

    private fun KtElement.isAlreadyCollected(): Boolean {
        val thisElement = this
        return typesToShorten.any { shortenType ->
            shortenType.element.qualifier?.let { thisElement.isInsideOf(it) } == true
        } || qualifiersToShorten.any { shortenQualifier ->
            thisElement.isInsideOf(shortenQualifier.element.receiverExpression)
        }
    }

    private fun addElementToShorten(elementInfoToShorten: ElementToShorten) {
        val qualifier = elementInfoToShorten.element.getQualifier() ?: return
        if (!qualifier.isAlreadyCollected()) {
            removeRedundantElements(qualifier)
            when (elementInfoToShorten) {
                is ShortenType -> typesToShorten.add(elementInfoToShorten)
                is ShortenQualifier -> qualifiersToShorten.add(elementInfoToShorten)
                is ShortenThisLabel -> labelsToShorten.add(elementInfoToShorten)
            }
        }
    }

    private val KtElement.inSelection: Boolean
        get() = when (this) {
            is KtUserType -> inSelection
            is KtDotQualifiedExpression -> inSelection
            is KtThisExpression -> inSelection

            else -> error("Unexpected ${this::class}")
        }

    /**
     * Checks whether type reference of [this] type is considered to be in the [selection] text range.
     *
     * Examples of calls:
     *
     * - `|foo.Bar<...>|` - true
     * - `foo.|Bar|<...>` - true
     * - `foo.|B|ar<...>` - true
     * - `|foo|.Bar<...>` - false
     * - `foo.Bar|<...>|` - false
     */
    private val KtUserType.inSelection: Boolean
        get() {
            val typeReference = referenceExpression ?: return false
            return typeReference.textRange.intersects(selection)
        }

    /**
     * Checks whether callee reference of [this] qualified expression is considered to be in the [selection] text range.
     *
     * Examples of calls:
     *
     *  - `|foo.bar()|` - true
     * - `foo.|bar|()` - true
     * - `foo.|b|ar()` - true
     * - `|foo|.bar()` - false
     * - `foo.bar|(...)|` - false
     */
    private val KtDotQualifiedExpression.inSelection: Boolean
        get() {
            val selectorReference = getQualifiedElementSelector() ?: return false
            return selectorReference.textRange.intersects(selection)
        }

    private val KtThisExpression.inSelection: Boolean
        get() = textRange.intersects(selection)

    private val ClassId.outerClassesWithSelf: Sequence<ClassId>
        get() = generateSequence(this) { it.outerClassId }

    private val KtElement.qualifiedElementsWithSelf: Sequence<KtElement>
        get() = when (this) {
            is KtUserType -> qualifiedTypesWithSelf
            is KtDotQualifiedExpression -> qualifiedExpressionsWithSelf
            else -> error("Unexpected ${this::class}")
        }

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

private class KDocQualifiersToShortenCollector(
    private val analysisSession: KaFirSession,
    private val selection: TextRange,
    private val additionalImports: AdditionalImports,
    private val classShortenStrategy: (FirClassLikeSymbol<*>) -> ShortenStrategy,
    private val callableShortenStrategy: (FirCallableSymbol<*>) -> ShortenStrategy,
) : KtVisitorVoid() {
    val kDocQualifiersToShorten: MutableList<ShortenKDocQualifier> = mutableListOf()
    override fun visitElement(element: PsiElement) {
        if (!element.textRange.intersects(selection)) return

        if (!selection.contains(element.textRange) || element !is KDocName) {
            element.acceptChildren(this)
            return
        }

        if (element.getQualifier() == null) return

        val shouldShortenKDocQualifier = shouldShortenKDocQualifier(
            element,
            additionalImports,
            classShortenStrategy = { classShortenStrategy(it) },
            callableShortenStrategy = { callableShortenStrategy(it) },
        )
        if (shouldShortenKDocQualifier) {
            addKDocQualifierToShorten(element)
        } else {
            element.acceptChildren(this)

            if (element.getQualifier()?.getNameText() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE) {
                addKDocQualifierToShorten(element)
            }
        }
    }

    private fun shouldShortenKDocQualifier(
        kDocName: KDocName,
        additionalImports: AdditionalImports,
        classShortenStrategy: (FirClassLikeSymbol<*>) -> ShortenStrategy,
        callableShortenStrategy: (FirCallableSymbol<*>) -> ShortenStrategy,
    ): Boolean {
        val fqName = kDocName.getQualifiedNameAsFqName().dropFakeRootPrefixIfPresent()

        // KDocs are only shortened if they are available without imports, so `additionalImports` contain all the imports to add
        if (fqName.isInNewImports(additionalImports)) return true

        val resolvedSymbols = with(analysisSession) {
            val shortFqName = FqName.topLevel(fqName.shortName())
            val owner = kDocName.getContainingDoc().owner

            val contextElement = owner ?: kDocName.containingKtFile
            KDocReferenceResolver.resolveKdocFqName(useSiteSession, shortFqName, shortFqName, contextElement)
        }

        resolvedSymbols.firstIsInstanceOrNull<KaCallableSymbol>()?.firSymbol?.let { availableCallable ->
            return canShorten(fqName, availableCallable.callableId.asSingleFqName()) { callableShortenStrategy(availableCallable) }
        }

        resolvedSymbols.firstIsInstanceOrNull<KaClassLikeSymbol>()?.firSymbol?.let { availableClassifier ->
            return canShorten(fqName, availableClassifier.classId.asSingleFqName()) { classShortenStrategy(availableClassifier) }
        }

        return false
    }

    private fun canShorten(fqNameToShorten: FqName, fqNameOfAvailableSymbol: FqName, getShortenStrategy: () -> ShortenStrategy): Boolean =
        fqNameToShorten == fqNameOfAvailableSymbol && getShortenStrategy() != ShortenStrategy.DO_NOT_SHORTEN

    private fun FqName.isInNewImports(additionalImports: AdditionalImports): Boolean =
        this in additionalImports.simpleImports || this.parent() in additionalImports.starImports

    private fun addKDocQualifierToShorten(kDocName: KDocName) {
        kDocQualifiersToShorten.add(ShortenKDocQualifier(kDocName))
    }
}

private class ShortenCommandImpl(
    override val targetFile: SmartPsiElementPointer<KtFile>,
    override val importsToAdd: Set<FqName>,
    override val starImportsToAdd: Set<FqName>,
    override val listOfTypeToShortenInfo: List<TypeToShortenInfo>,
    override val listOfQualifierToShortenInfo: List<QualifierToShortenInfo>,
    override val thisLabelsToShorten: List<ThisLabelToShortenInfo>,
    override val kDocQualifiersToShorten: List<SmartPsiElementPointer<KDocName>>,
) : ShortenCommand

private fun KtUserType.hasFakeRootPrefix(): Boolean =
    qualifier?.referencedName == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

private fun KtDotQualifiedExpression.hasFakeRootPrefix(): Boolean =
    (receiverExpression as? KtNameReferenceExpression)?.getReferencedName() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

internal fun KtSimpleNameExpression.getDotQualifiedExpressionForSelector(): KtDotQualifiedExpression? =
    getQualifiedElement() as? KtDotQualifiedExpression

private fun KtElement.getQualifier(): KtElement? = when (this) {
    is KtUserType -> qualifier
    is KtDotQualifiedExpression -> receiverExpression
    is KtThisExpression -> labelQualifier
    else -> error("Unexpected ${this::class}")
}

/**
 * N.B. We don't use [containingClassOrObject] because it works only for [KtDeclaration]s,
 * and also check only the immediate (direct) parent.
 *
 * For this function, we want to find the parent [KtClassOrObject] declaration no matter
 * how far it is from the element.
 */
private fun KtElement.findClassOrObjectParent(): KtClassOrObject? = parentOfType()

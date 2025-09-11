/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeParameterSupertype
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.getNestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.wrapNestedClassifierScopeWithSubstitutionForSuperType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

class FirSupertypeResolverProcessor(session: FirSession, scopeSession: ScopeSession) : FirTransformerBasedResolveProcessor(
    session, scopeSession, FirResolvePhase.SUPER_TYPES
) {
    override val transformer: FirSupertypeResolverTransformer = FirSupertypeResolverTransformer(session, scopeSession)
}

class FirSupertypeResolverTransformer(
    override val session: FirSession,
    scopeSession: ScopeSession,
) : FirAbstractPhaseTransformer<Any?>(FirResolvePhase.SUPER_TYPES) {
    private val supertypeComputationSession = SupertypeComputationSession()

    private val supertypeResolverVisitor = FirSupertypeResolverVisitor(session, supertypeComputationSession, scopeSession)
    private val applySupertypesTransformer = FirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession)

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            file.accept(supertypeResolverVisitor, null)
            supertypeComputationSession.breakLoops(session, supertypeResolverVisitor.localClassesNavigationInfo)
            file.transform(applySupertypesTransformer, null)
        }
    }
}

fun <F : FirClassLikeDeclaration> F.runSupertypeResolvePhaseForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<FirScope>,
    localClassesNavigationInfo: LocalClassesNavigationInfo,
    useSiteFile: FirFile,
    containingDeclarations: List<FirDeclaration>,
): F {
    @OptIn(FirImplementationDetail::class)
    val supertypeComputationSession = session.jumpingPhaseComputationSessionForLocalClassesProvider.superTypesPhaseSession()
    val supertypeResolverVisitor = FirSupertypeResolverVisitor(
        session, supertypeComputationSession, scopeSession,
        currentScopeList.toPersistentList(),
        localClassesNavigationInfo,
        useSiteFile,
        containingDeclarations,
    )

    this.accept(supertypeResolverVisitor, null)
    supertypeComputationSession.breakLoops(session, localClassesNavigationInfo)

    val applySupertypesTransformer = FirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession)
    return this.transform<F, Nothing?>(applySupertypesTransformer, null)
}

private class FirApplySupertypesTransformer(
    private val supertypeComputationSession: SupertypeComputationSession,
    private val session: FirSession,
    private val scopeSession: ScopeSession,
) : FirDefaultTransformer<Any?>() {

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    private fun transformDeclarationContent(declaration: FirDeclaration, data: Any?): FirDeclaration {
        return declaration.transformChildren(this, data) as FirDeclaration
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        return withFileAnalysisExceptionWrapping(file) {
            transformDeclarationContent(file, null) as FirFile
        }
    }

    override fun transformReplSnippet(replSnippet: FirReplSnippet, data: Any?): FirReplSnippet {
        return transformDeclarationContent(replSnippet, null) as FirReplSnippet
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
        applyResolvedSupertypesToClass(regularClass)

        return transformDeclarationContent(regularClass, null) as FirRegularClass
    }

    private fun applyResolvedSupertypesToClass(firClass: FirClass) {
        if (firClass.superTypeRefs.any { it !is FirResolvedTypeRef || it is FirImplicitBuiltinTypeRef }) {
            val supertypeRefs = supertypeComputationSession.getResolvedSupertypeRefs(firClass)
                .map { supertypeComputationSession.expandTypealiasInPlace(it, session) }
            firClass.replaceSuperTypeRefs(supertypeRefs)
        }

        session.platformSupertypeUpdater?.updateSupertypesIfNeeded(firClass, scopeSession)
    }


    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): FirStatement {
        applyResolvedSupertypesToClass(anonymousObject)

        return anonymousObject.transformChildren(this, data) as FirAnonymousObject
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Any?): FirStatement {
        if (typeAlias.expandedTypeRef is FirResolvedTypeRef) {
            return typeAlias
        }
        val resolvedTypeRef = supertypeComputationSession.getResolvedExpandedTypeRef(typeAlias)
        val resolvedExpandedTypeRef = supertypeComputationSession.expandTypealiasInPlace(resolvedTypeRef, session)
        typeAlias.replaceExpandedTypeRef(resolvedExpandedTypeRef)
        return typeAlias
    }
}

private fun FirClassLikeDeclaration.typeParametersScope(): FirScope? {
    if (typeParameters.isEmpty()) return null
    return FirMemberTypeParameterScope(this)
}

private fun createOtherScopesForNestedClassesOrCompanion(
    klass: FirClass,
    session: FirSession,
    scopeSession: ScopeSession,
    supertypeComputationSession: SupertypeComputationSession,
    withCompanionScopes: Boolean,
): Collection<FirScope> =
    mutableListOf<FirScope>().apply {
        // Note: from higher priority to lower priority
        // See also: BodyResolveContext.withScopesForClass
        addIfNotNull(session.nestedClassifierScope(klass))
        if (withCompanionScopes) {
            val companionObjects = klass.declarations.filterIsInstance<FirRegularClass>().filter { it.isCompanion }
            for (companionObject in companionObjects) {
                addIfNotNull(session.nestedClassifierScope(companionObject))
            }
        }
        lookupSuperTypes(
            klass,
            lookupInterfaces = false, deep = true, substituteTypes = true, useSiteSession = session,
            supertypeSupplier = supertypeComputationSession.supertypesSupplier
        ).mapNotNullTo(this) {
            it.lookupTag.getNestedClassifierScope(session, scopeSession)
                ?.wrapNestedClassifierScopeWithSubstitutionForSuperType(it, session)
        }
        // The type parameters scope has already been
        // added by this time;
        // See: prepareScopes()
    }

open class FirSupertypeResolverVisitor(
    private val session: FirSession,
    private val supertypeComputationSession: SupertypeComputationSession,
    private val scopeSession: ScopeSession,
    private val scopeForLocalClass: PersistentList<FirScope>? = null,
    val localClassesNavigationInfo: LocalClassesNavigationInfo? = null,
    @property:PrivateForInline var useSiteFile: FirFile? = null,
    containingDeclarations: List<FirDeclaration> = emptyList(),
) : FirDefaultVisitor<Unit, Any?>() {
    private val supertypeGenerationExtensions = session.extensionService.supertypeGenerators

    @PrivateForInline
    val classDeclarationsStack: ArrayDeque<FirClass> = ArrayDeque()
    private var replSnippet: FirReplSnippet? = null

    init {
        containingDeclarations.forEach {
            if (it is FirClass) {
                @OptIn(PrivateForInline::class)
                classDeclarationsStack.add(it)
            }
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <R> withFile(file: FirFile, block: () -> R): R {
        val oldFile = useSiteFile
        try {
            useSiteFile = file
            return block()
        } finally {
            useSiteFile = oldFile
        }
    }

    private fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? {
        return symbol.moduleData.session.firProvider.getFirClassifierContainerFileIfAny(symbol)
    }

    override fun visitElement(element: FirElement, data: Any?) {}

    private fun prepareFileScopes(file: FirFile): ScopePersistentList {
        return supertypeComputationSession.getOrPutFileScope(file) {
            createImportingScopes(file, session, scopeSession).asReversed().toPersistentList()
        }
    }

    private fun prepareReplScopes(replSnippet: FirReplSnippet): ScopePersistentList {
        return buildList {
            val symbol = replSnippet.symbol
            val file = symbol.moduleData.session.firProvider.getFirReplSnippetContainerFile(symbol)
            if (file != null) addAll(prepareFileScopes(file))

            // TODO: robuster matching and error reporting on no extension (KT-72969)
            for (resolveExt in session.extensionService.replSnippetResolveExtensions) {
                val scope = resolveExt.getSnippetScope(replSnippet, session)
                if (scope != null) add(scope)
            }
        }.toPersistentList()
    }

    private fun prepareScopeForNestedClasses(klass: FirClass, forStaticNestedClass: Boolean): ScopePersistentList {
        return if (forStaticNestedClass) {
            supertypeComputationSession.getOrPutScopeForStaticNestedClasses(klass) {
                calculateScopes(klass, withCompanionScopes = true, forStaticNestedClass = true)
            }
        } else {
            supertypeComputationSession.getOrPutScopeForNestedClasses(klass) {
                calculateScopes(klass, withCompanionScopes = true, forStaticNestedClass = false)
            }
        }
    }

    private fun prepareScopeForCompanion(klass: FirClass): ScopePersistentList {
        return supertypeComputationSession.getOrPutScopeForCompanion(klass) {
            calculateScopes(klass, withCompanionScopes = false, forStaticNestedClass = true)
        }
    }

    private fun calculateScopes(
        outerClass: FirClass,
        withCompanionScopes: Boolean,
        forStaticNestedClass: Boolean,
    ): PersistentList<FirScope> {
        resolveAllSupertypesForOuterClass(outerClass)
        return prepareScopes(outerClass, forStaticNestedClass).pushAll(
            createOtherScopesForNestedClassesOrCompanion(
                klass = outerClass,
                session = session,
                scopeSession = scopeSession,
                supertypeComputationSession = supertypeComputationSession,
                withCompanionScopes = withCompanionScopes,
            )
        )
    }

    /**
     * Resolve all super types. [outerClass] is used as an outer scope for nested class or companion
     */
    protected open fun resolveAllSupertypesForOuterClass(outerClass: FirClass) {
        resolveAllSupertypes(outerClass, outerClass.superTypeRefs)
    }

    private fun resolveAllSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        supertypeRefs: List<FirTypeRef>,
        visited: MutableSet<FirClassLikeDeclaration> = mutableSetOf(),
    ) {
        if (!visited.add(classLikeDeclaration)) return
        val supertypes: List<ConeKotlinType> =
            resolveSpecificClassLikeSupertypes(classLikeDeclaration, supertypeRefs, resolveRecursively = true).map { it.coneType }

        for (supertype in supertypes) {
            if (supertype !is ConeClassLikeType) continue
            val supertypeModuleSession = supertype.toSymbol(session)?.moduleData?.session ?: continue
            val fir = supertype.lookupTag.toSymbol(supertypeModuleSession)?.fir ?: continue
            resolveAllSupertypes(fir, supertypeComputationSession.supertypeRefs(fir, session), visited)
        }
    }

    private fun prepareScopes(classLikeDeclaration: FirClassLikeDeclaration, forStaticNestedClass: Boolean): PersistentList<FirScope> {
        val classId = classLikeDeclaration.symbol.classId
        val classModuleSession = classLikeDeclaration.moduleData.session

        val result = when {
            classLikeDeclaration.isLocal -> {
                // Typically, local class-like declarations (class, typealias) should be treated specially and supplied with localClassesNavigationInfo.
                // But it seems to be too strict to add an assertion here
                if (localClassesNavigationInfo == null) return persistentListOf()

                val parent = localClassesNavigationInfo.parentForClass[classLikeDeclaration]

                when {
                    parent != null && parent is FirClass -> prepareScopeForNestedClasses(parent, forStaticNestedClass)
                    else -> scopeForLocalClass ?: return persistentListOf()
                }
            }
            (classLikeDeclaration as? FirRegularClass)?.isCompanion == true -> {
                val outerClassFir = classModuleSession.firProvider.getContainingClass(classLikeDeclaration.symbol)?.fir as? FirRegularClass
                prepareScopeForCompanion(outerClassFir ?: return persistentListOf())
            }
            classId.isNestedClass -> {
                val outerClassFir = classModuleSession.firProvider.getContainingClass(classLikeDeclaration.symbol)?.fir as? FirRegularClass
                val isStatic = !classLikeDeclaration.isInner
                prepareScopeForNestedClasses(outerClassFir ?: return persistentListOf(), isStatic || forStaticNestedClass)
            }
            replSnippet != null -> prepareReplScopes(replSnippet!!)
            else -> getFirClassifierContainerFileIfAny(classLikeDeclaration.symbol)?.let(::prepareFileScopes) ?: persistentListOf()
        }

        return when {
            forStaticNestedClass -> result
            else -> result.pushIfNotNull(classLikeDeclaration.typeParametersScope())
        }
    }

    private fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolveSuperTypeRefs: (FirSpecificTypeResolverTransformer, TypeResolutionConfiguration) -> List<FirResolvedTypeRef>,
    ): List<FirResolvedTypeRef> {
        when (val status = supertypeComputationSession.getSupertypesComputationStatus(classLikeDeclaration)) {
            is SupertypeComputationStatus.Computed -> return status.supertypeRefs
            is SupertypeComputationStatus.Computing -> return listOf(
                createErrorTypeRef(
                    classLikeDeclaration.source,
                    "Loop in supertype definition for ${classLikeDeclaration.symbol.classId}",
                    if (classLikeDeclaration is FirTypeAlias) DiagnosticKind.RecursiveTypealiasExpansion else DiagnosticKind.LoopInSupertype
                )
            )
            SupertypeComputationStatus.NotComputed -> {}
        }

        supertypeComputationSession.startComputingSupertypes(classLikeDeclaration)
        val scopes = prepareScopes(classLikeDeclaration, forStaticNestedClass = false)

        val transformer = FirSpecificTypeResolverTransformer(
            session,
            supertypeSupplier = supertypeComputationSession.supertypesSupplier,
            // They will be unwrapped later during this phase.
            expandTypeAliases = false,
        )

        val newUseSiteFile =
            if (classLikeDeclaration.isLocal) @OptIn(PrivateForInline::class) useSiteFile
            else session.firProvider.getFirClassifierContainerFileIfAny(classLikeDeclaration.symbol)

        @OptIn(PrivateForInline::class)
        val resolvedTypesRefs =
            resolveSuperTypeRefs(
                transformer,
                TypeResolutionConfiguration(scopes, classDeclarationsStack, newUseSiteFile),
            )

        supertypeComputationSession.storeSupertypes(classLikeDeclaration, resolvedTypesRefs)
        return resolvedTypesRefs
    }

    private fun visitDeclarationContent(declaration: FirDeclaration, data: Any?) {
        declaration.acceptChildren(this, data)
    }

    inline fun <T> withClass(firClass: FirClass, body: () -> T): T {
        @OptIn(PrivateForInline::class)
        return withClassDeclarationCleanup(classDeclarationsStack, firClass) {
            body()
        }
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: Any?) {
        withClass(regularClass) {
            resolveSpecificClassLikeSupertypes(regularClass, regularClass.superTypeRefs, resolveRecursively = true)
            visitDeclarationContent(regularClass, null)
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?) {
        withClass(anonymousObject) {
            resolveSpecificClassLikeSupertypes(anonymousObject, anonymousObject.superTypeRefs, resolveRecursively = true)
            visitDeclarationContent(anonymousObject, null)
        }
    }

    override fun visitReplSnippet(replSnippet: FirReplSnippet, data: Any?) {
        val original = this.replSnippet
        this.replSnippet = replSnippet
        try {
            visitDeclarationContent(replSnippet, data)
        } finally {
            this.replSnippet = original
        }
    }

    /**
     * The function won't call supertypeRefs on classLikeDeclaration directly
     */
    fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        supertypeRefs: List<FirTypeRef>,
        resolveRecursively: Boolean,
    ): List<FirResolvedTypeRef> {
        return resolveSpecificClassLikeSupertypes(classLikeDeclaration) { transformer, configuration ->
            supertypeRefs.mapTo(mutableListOf()) {
                val superTypeRef = it.transform<FirTypeRef, TypeResolutionConfiguration>(transformer, configuration)
                val typeParameterType = superTypeRef.coneTypeSafe<ConeTypeParameterType>()
                val typealiasSymbol = superTypeRef.coneTypeSafe<ConeClassLikeType>()?.toTypeAliasSymbol(session)
                if (resolveRecursively && typealiasSymbol != null) {
                    // Jump to typealiases in supertypes of class-like types.
                    // We need to make sure that by the time we want to fully expand typealiases in supertypes
                    // we already have analyzed them.
                    visitTypeAlias(typealiasSymbol.fir, null)
                }
                when {
                    typeParameterType != null ->
                        buildErrorTypeRef {
                            source = superTypeRef.source
                            diagnostic = ConeTypeParameterSupertype(typeParameterType.lookupTag.typeParameterSymbol)
                        }
                    superTypeRef !is FirResolvedTypeRef ->
                        createErrorTypeRef(
                            superTypeRef.source,
                            "Unresolved super-type: ${superTypeRef.render()}",
                            DiagnosticKind.UnresolvedSupertype
                        )
                    else ->
                        superTypeRef
                }
            }.also {
                addSupertypesFromExtensions(classLikeDeclaration, it, transformer, configuration)
                /**
                 * TODO: Supertype resolution for generated classes is not supported in AA (KT-69404)
                 * `resolveRecursively` is set to `true` in the compiler and to `false` in the AA
                 */
                @OptIn(PrivateForInline::class)
                if (resolveRecursively && configuration.useSiteFile != null && classLikeDeclaration is FirRegularClass) {
                    addSupertypesToGeneratedNestedClasses(classLikeDeclaration, transformer, configuration)
                }
            }
        }
    }

    private fun addSupertypesFromExtensions(
        klass: FirClassLikeDeclaration,
        supertypeRefs: MutableList<FirResolvedTypeRef>,
        typeResolveTransformer: FirSpecificTypeResolverTransformer,
        configuration: TypeResolutionConfiguration,
    ) {
        if (supertypeGenerationExtensions.isEmpty()) return
        val typeResolveService = TypeResolveServiceForPlugins(typeResolveTransformer, configuration)
        for (extension in supertypeGenerationExtensions) {
            if (extension.needTransformSupertypes(klass)) {
                extension.computeAdditionalSupertypes(klass, supertypeRefs, typeResolveService).mapTo(supertypeRefs) {
                    it.toFirResolvedTypeRef(klass.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated))
                }
            }
        }
    }

    private fun addSupertypesToGeneratedNestedClasses(
        klass: FirRegularClass,
        typeResolveTransformer: FirSpecificTypeResolverTransformer,
        configuration: TypeResolutionConfiguration,
    ) {
        if (supertypeGenerationExtensions.isEmpty()) return
        val typeResolveService = TypeResolveServiceForPlugins(typeResolveTransformer, configuration)
        val generatedNestedClasses = klass.generatedNestedClassifiers(session)
        for (nestedClass in generatedNestedClasses) {
            if (nestedClass !is FirRegularClass) continue
            requireWithAttachment(
                nestedClass.superTypeRefs.all { it is FirResolvedTypeRef },
                { "Supertypes of generated class should be resolved"}
            ) {
                val unresolvedTypes = nestedClass.superTypeRefs.filter { it !is FirResolvedTypeRef }
                withEntry("Unresolved types", unresolvedTypes.joinToString(", ") { it.render() })
            }

            @Suppress("UNCHECKED_CAST")
            val superTypes = nestedClass.superTypeRefs.toMutableList() as MutableList<FirResolvedTypeRef>

            var someTypesWereGenerated = false

            for (extension in supertypeGenerationExtensions) {
                if (extension.needTransformSupertypes(nestedClass)) {
                    @OptIn(ExperimentalSupertypesGenerationApi::class)
                    val newSupertypes = extension.computeAdditionalSupertypesForGeneratedNestedClass(
                        nestedClass, typeResolveService
                    )
                    if (newSupertypes.isNotEmpty()) {
                        someTypesWereGenerated = true
                        superTypes += newSupertypes.map {
                            it.toFirResolvedTypeRef(klass.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated))
                        }
                    }
                }
            }
            if (someTypesWereGenerated && superTypes.isNotEmpty()) {
                superTypes.removeIf { it.coneType.isAny }
            }
            nestedClass.replaceSuperTypeRefs(superTypes)
        }
    }

    private class TypeResolveServiceForPlugins(
        val typeResolveTransformer: FirSpecificTypeResolverTransformer,
        val configuration: TypeResolutionConfiguration,
    ) : FirSupertypeGenerationExtension.TypeResolveService() {
        override fun resolveUserType(type: FirUserTypeRef): FirResolvedTypeRef {
            return typeResolveTransformer.withBareTypes(allowed = true) {
                type.transform(typeResolveTransformer, configuration)
            }
        }
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Any?) {
        resolveTypeAliasSupertype(typeAlias, typeAlias.expandedTypeRef, resolveRecursively = true)
    }

    fun resolveTypeAliasSupertype(
        typeAlias: FirTypeAlias,
        expandedTypeRef: FirTypeRef,
        resolveRecursively: Boolean,
    ): List<FirResolvedTypeRef> {
        if (expandedTypeRef is FirResolvedTypeRef) {
            return listOf(expandedTypeRef)
        }

        return resolveSpecificClassLikeSupertypes(typeAlias) { transformer, scope ->
            val resolvedTypeRef = transformer.transformTypeRef(expandedTypeRef, scope)

            if (resolveRecursively) {
                fun visitNestedTypeAliases(type: ConeTypeProjection) {
                    val typeToCheck = type.type?.lowerBoundIfFlexible() as? ConeClassLikeType ?: return
                    val symbol = typeToCheck.lookupTag.toSymbol(session)
                    if (symbol is FirTypeAliasSymbol) {
                        visitTypeAlias(symbol.fir, null)
                    } else if (symbol is FirClassLikeSymbol) {
                        for (typeArgument in typeToCheck.typeArguments) {
                            visitNestedTypeAliases(typeArgument)
                        }
                    }
                }

                visitNestedTypeAliases(resolvedTypeRef.coneType)
            }

            listOf(resolvedTypeRef)
        }
    }

    override fun visitFile(file: FirFile, data: Any?) {
        withFile(file) {
            visitDeclarationContent(file, null)
        }
    }
}

private fun createErrorTypeRef(sourceElement: KtSourceElement?, message: String, kind: DiagnosticKind) = buildErrorTypeRef {
    source = sourceElement
    diagnostic = ConeSimpleDiagnostic(message, kind)
}

open class SupertypeComputationSession {
    private val fileScopesMap = hashMapOf<FirFile, ScopePersistentList>()
    private val scopesForNestedClassesMap = hashMapOf<FirClass, ScopePersistentList>()
    private val scopesForStaticNestedClassesMap = hashMapOf<FirClass, ScopePersistentList>()
    private val scopesForCompanionMap = hashMapOf<FirClass, ScopePersistentList>()
    private val supertypeStatusMap = linkedMapOf<FirClassLikeDeclaration, SupertypeComputationStatus>()

    val supertypesSupplier: SupertypeSupplier = object : SupertypeSupplier() {
        override fun forClass(firClass: FirClass, useSiteSession: FirSession): List<ConeClassLikeType> {
            val typeRefsFromSession = (getSupertypesComputationStatus(firClass) as? SupertypeComputationStatus.Computed)?.supertypeRefs
            val typeRefsToReturn = typeRefsFromSession ?: getResolvedSuperTypeRefsForOutOfSessionDeclaration(firClass, useSiteSession)
            return typeRefsToReturn.mapNotNull { it.coneTypeSafe<ConeClassLikeType>() }
        }

        override fun expansionForTypeAlias(typeAlias: FirTypeAlias, useSiteSession: FirSession): ConeClassLikeType? {
            if (typeAlias.expandedTypeRef is FirResolvedTypeRef) {
                return typeAlias.expandedConeType
            }

            val typeRefsFromSession = (getSupertypesComputationStatus(typeAlias) as? SupertypeComputationStatus.Computed)?.supertypeRefs
            val typeRefsToReturn = (typeRefsFromSession ?: getResolvedSuperTypeRefsForOutOfSessionDeclaration(typeAlias, useSiteSession))
            return typeRefsToReturn.firstOrNull()?.coneTypeSafe()
        }
    }

    fun getSupertypesComputationStatus(classLikeDeclaration: FirClassLikeDeclaration): SupertypeComputationStatus =
        supertypeStatusMap[classLikeDeclaration] ?: SupertypeComputationStatus.NotComputed

    fun getOrPutFileScope(file: FirFile, scope: () -> ScopePersistentList): ScopePersistentList =
        fileScopesMap.getOrPut(file) { scope() }

    fun getOrPutScopeForNestedClasses(klass: FirClass, scope: () -> ScopePersistentList): ScopePersistentList =
        scopesForNestedClassesMap.getOrPut(klass) { scope() }

    fun getOrPutScopeForStaticNestedClasses(klass: FirClass, scope: () -> ScopePersistentList): ScopePersistentList =
        scopesForStaticNestedClassesMap.getOrPut(klass) { scope() }

    fun getOrPutScopeForCompanion(klass: FirClass, scope: () -> ScopePersistentList): ScopePersistentList =
        scopesForCompanionMap.getOrPut(klass) { scope() }

    fun startComputingSupertypes(classLikeDeclaration: FirClassLikeDeclaration) {
        require(supertypeStatusMap[classLikeDeclaration] == null) {
            "Unexpected in startComputingSupertypes supertype status for $classLikeDeclaration: ${supertypeStatusMap[classLikeDeclaration]}"
        }

        supertypeStatusMap[classLikeDeclaration] = SupertypeComputationStatus.Computing
    }

    fun storeSupertypes(classLikeDeclaration: FirClassLikeDeclaration, resolvedTypesRefs: List<FirResolvedTypeRef>) {
        require(supertypeStatusMap[classLikeDeclaration] is SupertypeComputationStatus.Computing) {
            "Unexpected in storeSupertypes supertype status for $classLikeDeclaration: ${supertypeStatusMap[classLikeDeclaration]}"
        }

        supertypeStatusMap[classLikeDeclaration] = SupertypeComputationStatus.Computed(resolvedTypesRefs)
        newClassifiersForBreakingLoops.add(classLikeDeclaration)
    }

    private val newClassifiersForBreakingLoops = mutableListOf<FirClassLikeDeclaration>()

    /**
     * @param supertypeRefs a collection where at least one element is [FirErrorTypeRef] for looped references
     */
    protected open fun reportLoopErrorRefs(classLikeDeclaration: FirClassLikeDeclaration, supertypeRefs: List<FirResolvedTypeRef>) {
        supertypeStatusMap[classLikeDeclaration] = SupertypeComputationStatus.Computed(supertypeRefs)
    }

    protected open fun getResolvedSuperTypeRefsForOutOfSessionDeclaration(
        classLikeDeclaration: FirClassLikeDeclaration,
        useSiteSession: FirSession,
    ): List<FirResolvedTypeRef> = when (classLikeDeclaration) {
        is FirClass -> classLikeDeclaration.superTypeRefs.filterIsInstance<FirResolvedTypeRef>()
        is FirTypeAlias -> listOfNotNull(classLikeDeclaration.expandedTypeRef as? FirResolvedTypeRef)
    }

    /**
     * This method is used only to resolve the super type hierarchy. It may return an empty list if the hierarchy is fully resolved.
     */
    open fun supertypeRefs(declaration: FirClassLikeDeclaration, useSiteSession: FirSession): List<FirTypeRef> = when (declaration) {
        is FirRegularClass -> declaration.superTypeRefs
        is FirTypeAlias -> listOf(declaration.expandedTypeRef)
        else -> emptyList()
    }

    /**
     * The main purpose of this function is to find and report loops in supertypes for combined class / typealias hierarchy.
     *
     * In principle, we solve here a classic task of finding loops in an oriented graph, with some specifics:
     *
     * - there are two types of vertices: classes and typealiases
     * - there are two types of edges: "solid" describes subtyping or typealias main classifier, "dotted" describes type arguments
     * - an oriented loop consisting of "solid" edges only is important for us anyway
     * - an oriented loop including "dotted" edges is important for us if all vertices are typealiases
     *
     * You can find some green examples in testData/diagnostics/tests/cyclicHierarchy/withTypeAlias0.kt.
     * You can find some red examples in testData/diagnostics/tests/cyclicHierarchy/withTypeAlias.kt, withTypeAlias2.kt.
     *
     * @param declaration declaration to be checked for loops
     * @param visited visited declarations during the current loop search (DFS tree)
     * @param looped declarations inside loop
     * @param pathOrderedSet declaration ordered set (in visit order) on a current path (DFS branch).
     * @param localClassesNavigationInfo auxiliary parameter to find local classes parents
     *
     * The parameters [visited], [looped], [pathOrderedSet] exist
     * to avoid repeated memory allocation for each search, see `LLFirSupertypeComputationSession`.
     */
    protected fun breakLoopFor(
        declaration: FirClassLikeDeclaration,
        session: FirSession,
        visited: MutableSet<FirClassLikeDeclaration>, // always empty for LL FIR
        looped: MutableSet<FirClassLikeDeclaration>, // always empty for LL FIR
        pathOrderedSet: LinkedHashSet<FirClassLikeDeclaration>,
        localClassesNavigationInfo: LocalClassesNavigationInfo?,
    ) {
        require(pathOrderedSet.isEmpty()) { "Path ordered set should be empty before starting" }

        /**
         * Local function that operates as a DFS node during loop search.
         *
         * @param classLikeDeclaration DFS node declaration, if any
         * @param wasSubtypingInvolved loop contains at least one class
         * @param wereTypeArgumentsInvolved loop contains at least one "dotted" edge
         */
        fun checkIsInLoop(
            classLikeDeclaration: FirClassLikeDeclaration?,
            wasSubtypingInvolved: Boolean,
            wereTypeArgumentsInvolved: Boolean,
        ) {
            if (classLikeDeclaration == null) return
            require(!wasSubtypingInvolved || !wereTypeArgumentsInvolved) {
                "This must hold by induction, because otherwise such a loop is allowed"
            }

            val supertypeStatus = supertypeStatusMap[classLikeDeclaration]
            val supertypeRefs: List<FirResolvedTypeRef> = if (supertypeStatus != null) {
                require(supertypeStatus is SupertypeComputationStatus.Computed) {
                    "Expected computed supertypes in breakLoops for ${classLikeDeclaration.symbol.classId}"
                }

                supertypeStatus.supertypeRefs
            } else {
                getResolvedSuperTypeRefsForOutOfSessionDeclaration(classLikeDeclaration, session)
            }

            if (classLikeDeclaration in visited) {
                if (classLikeDeclaration in pathOrderedSet) {
                    looped.add(classLikeDeclaration)
                    looped.addAll(pathOrderedSet.reversed().takeWhile { element -> element != classLikeDeclaration })
                }

                return
            }

            val declarationIsAdded = pathOrderedSet.add(classLikeDeclaration)
            require(declarationIsAdded) { "The considered declaration should be unique" }
            visited.add(classLikeDeclaration)

            val classId = classLikeDeclaration.classId
            if (classId.isNestedClass) {
                val parentFir = when {
                    !classLikeDeclaration.isLocal -> session.firProvider.getContainingClass(classLikeDeclaration.symbol)?.fir
                    localClassesNavigationInfo != null -> localClassesNavigationInfo.parentForClass[classLikeDeclaration]
                    else -> error("Couldn't retrieve the parent of a local class because there's no `LocalClassesNavigationInfo`")
                }
                if (parentFir is FirClassLikeDeclaration) {
                    checkIsInLoop(parentFir, wasSubtypingInvolved, wereTypeArgumentsInvolved)
                }
            }

            val isTypeAlias = classLikeDeclaration is FirTypeAlias
            val isSubtypingCurrentlyInvolved = !isTypeAlias

            // This is an optimization that prevents collecting
            // loops we don't want to report anyway.
            if (wereTypeArgumentsInvolved && isSubtypingCurrentlyInvolved) {
                pathOrderedSet.remove(classLikeDeclaration)
                // This declaration can be visited once more, for example, to find loops beginning with it
                visited.remove(classLikeDeclaration)
                return
            }

            val isSubtypingInvolved = wasSubtypingInvolved || isSubtypingCurrentlyInvolved
            var isErrorInSupertypesFound = false
            val resultSupertypeRefs = mutableListOf<FirResolvedTypeRef>()
            for (supertypeRef in supertypeRefs) {
                if (isTypeAlias) {
                    // For case like typealias S = @S SomeAnnotation
                    for (annotation in supertypeRef.annotations) {
                        val resolvedType = annotation.resolvedType as? ConeClassLikeType ?: continue
                        val annotationClassLikeDeclaration = resolvedType.lookupTag.toSymbol(session)?.fir
                        checkIsInLoop(annotationClassLikeDeclaration, wasSubtypingInvolved, wereTypeArgumentsInvolved)
                    }
                }

                // rhs value is required only for the Analysis API, as in the CLI mode there are no invisible dependencies
                val supertypeFir = supertypeRef.firClassLike(session) ?: supertypeRef.firClassLike(classLikeDeclaration.moduleData.session)
                checkIsInLoop(supertypeFir, isSubtypingInvolved, wereTypeArgumentsInvolved)

                // This is an optimization that prevents collecting
                // loops we don't want to report anyway.
                if (!isSubtypingInvolved) {
                    val areTypeArgumentsCurrentlyInvolved = true

                    fun checkTypeArgumentsRecursively(type: ConeKotlinType, visitedTypes: MutableSet<ConeKotlinType>) {
                        if (type in visitedTypes) return
                        visitedTypes += type
                        for (typeArgument in type.typeArguments) {
                            val typeToCheck = typeArgument.type?.lowerBoundIfFlexible() as? ConeClassLikeType ?: continue
                            checkIsInLoop(
                                typeToCheck.lookupTag.toSymbol(session)?.fir,
                                wasSubtypingInvolved, areTypeArgumentsCurrentlyInvolved,
                            )
                            checkTypeArgumentsRecursively(typeToCheck, visitedTypes)
                        }
                    }

                    checkTypeArgumentsRecursively(supertypeRef.coneType, mutableSetOf())
                }

                resultSupertypeRefs.add(
                    if (classLikeDeclaration in looped && supertypeRef !is FirImplicitBuiltinTypeRef) {
                        isErrorInSupertypesFound = true
                        createErrorTypeRef(
                            supertypeRef.source,
                            // A loop may have been caused by the outer declaration, not necessarily `supertypeRef`
                            "Loop in supertypes involving ${classLikeDeclaration.symbol.classId}",
                            if (isTypeAlias) DiagnosticKind.RecursiveTypealiasExpansion else DiagnosticKind.LoopInSupertype
                        )
                    } else {
                        supertypeRef
                    }
                )
            }

            if (isErrorInSupertypesFound) {
                reportLoopErrorRefs(classLikeDeclaration, resultSupertypeRefs)
            }

            pathOrderedSet.remove(classLikeDeclaration)
        }

        checkIsInLoop(declaration, wasSubtypingInvolved = false, wereTypeArgumentsInvolved = false)
        require(pathOrderedSet.isEmpty()) { "Path ordered set should be empty after finishing" }
    }

    fun breakLoops(session: FirSession, localClassesNavigationInfo: LocalClassesNavigationInfo?) {
        val visitedClassLikeDecls = mutableSetOf<FirClassLikeDeclaration>()
        val loopedClassLikeDecls = mutableSetOf<FirClassLikeDeclaration>()
        val pathOrderedSet = LinkedHashSet<FirClassLikeDeclaration>()

        for (classifier in newClassifiersForBreakingLoops) {
            breakLoopFor(
                declaration = classifier,
                session = session,
                visited = visitedClassLikeDecls,
                looped = loopedClassLikeDecls,
                pathOrderedSet = pathOrderedSet,
                localClassesNavigationInfo = localClassesNavigationInfo,
            )
        }

        newClassifiersForBreakingLoops.clear()
    }

    fun getResolvedSupertypeRefs(classLikeDeclaration: FirClassLikeDeclaration): List<FirResolvedTypeRef> {
        val status = getSupertypesComputationStatus(classLikeDeclaration)
        require(status is SupertypeComputationStatus.Computed) {
            "Unexpected status at FirApplySupertypesTransformer: $status for ${classLikeDeclaration.symbol.classId}"
        }
        return status.supertypeRefs
    }

    fun getResolvedExpandedTypeRef(typeAlias: FirTypeAlias): FirTypeRef {
        val supertypeRefs = getResolvedSupertypeRefs(typeAlias)
        assert(supertypeRefs.size == 1) {
            "Expected single supertypeRefs, but found ${supertypeRefs.size} in ${typeAlias.symbol.classId}"
        }
        return supertypeRefs[0]
    }

    fun expandTypealiasInPlace(typeRef: FirTypeRef, session: FirSession): FirTypeRef {
        return when (typeRef) {
            is FirImplicitBuiltinTypeRef, is FirErrorTypeRef -> typeRef
            else -> when (val expanded = typeRef.coneType.fullyExpandedType(session, ::getResolvedExpandedType)) {
                typeRef.coneType -> typeRef
                else -> {
                    if (session.languageVersionSettings.getFlag(AnalysisFlags.expandTypeAliasesInTypeResolution)) {
                        expanded.let(typeRef::withReplacedConeType)
                    } else {
                        typeRef
                    }
                }
            }
        }
    }

    private fun getResolvedExpandedType(typeAlias: FirTypeAlias): ConeClassLikeType? =
        (typeAlias.expandedTypeRef.takeIf { it is FirResolvedTypeRef } ?: getResolvedExpandedTypeRef(typeAlias))
            .coneTypeSafe<ConeClassLikeType>()
}

sealed class SupertypeComputationStatus {
    object NotComputed : SupertypeComputationStatus()
    object Computing : SupertypeComputationStatus()

    class Computed(val supertypeRefs: List<FirResolvedTypeRef>) : SupertypeComputationStatus()
}

private typealias ScopePersistentList = PersistentList<FirScope>

private fun <E> PersistentList<E>.push(element: E): PersistentList<E> = add(0, element)
private fun <E> PersistentList<E>.pushAll(collection: Collection<E>): PersistentList<E> = addAll(0, collection)

private fun ScopePersistentList.pushIfNotNull(scope: FirScope?): ScopePersistentList = if (scope == null) this else push(scope)

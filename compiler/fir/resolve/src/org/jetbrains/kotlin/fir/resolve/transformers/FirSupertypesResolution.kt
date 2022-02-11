/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.supertypeGenerators
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isLocalClassOrAnonymousObject
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeParameterSupertype
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.getNestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.wrapNestedClassifierScopeWithSubstitutionForSuperType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.model.TypeArgumentMarker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirSupertypeResolverProcessor(session: FirSession, scopeSession: ScopeSession) :
    FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = FirSupertypeResolverTransformer(session, scopeSession)
}

/**
 * Interceptor needed by IDE to resolve in-air created declarations.
 */
interface FirProviderInterceptor {
    fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile?
    fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration?
}

open class FirSupertypeResolverTransformer(
    final override val session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractPhaseTransformer<Any?>(FirResolvePhase.SUPER_TYPES) {
    protected val supertypeComputationSession = SupertypeComputationSession()

    private val supertypeResolverVisitor = FirSupertypeResolverVisitor(session, supertypeComputationSession, scopeSession)
    private val applySupertypesTransformer = FirApplySupertypesTransformer(supertypeComputationSession)

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        checkSessionConsistency(file)
        file.accept(supertypeResolverVisitor, null)
        supertypeComputationSession.breakLoops(session)
        return file.transform(applySupertypesTransformer, null)
    }
}

fun <F : FirClassLikeDeclaration> F.runSupertypeResolvePhaseForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<FirScope>,
    localClassesNavigationInfo: LocalClassesNavigationInfo,
    firProviderInterceptor: FirProviderInterceptor?,
    useSiteFile: FirFile,
    containingDeclarations: List<FirDeclaration>,
): F {
    val supertypeComputationSession = SupertypeComputationSession()
    val supertypeResolverVisitor = FirSupertypeResolverVisitor(
        session, supertypeComputationSession, scopeSession,
        currentScopeList.toPersistentList(),
        localClassesNavigationInfo,
        firProviderInterceptor,
        useSiteFile,
        containingDeclarations,
    )

    this.accept(supertypeResolverVisitor, null)
    supertypeComputationSession.breakLoops(session)

    val applySupertypesTransformer = FirApplySupertypesTransformer(supertypeComputationSession)
    return this.transform<F, Nothing?>(applySupertypesTransformer, null)
}

open class FirApplySupertypesTransformer(
    private val supertypeComputationSession: SupertypeComputationSession
) : FirDefaultTransformer<Any?>() {
    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    protected open fun transformDeclarationContent(declaration: FirDeclaration, data: Any?): FirDeclaration {
        return declaration.transformChildren(this, null) as FirDeclaration
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        return transformDeclarationContent(file, null) as FirFile
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
        applyResolvedSupertypesToClass(regularClass)

        return transformDeclarationContent(regularClass, null) as FirRegularClass
    }

    private fun applyResolvedSupertypesToClass(firClass: FirClass) {
        if (firClass.superTypeRefs.any { it !is FirResolvedTypeRef || it is FirImplicitBuiltinTypeRef }) {
            val supertypeRefs = getResolvedSupertypeRefs(firClass)

            // TODO: Replace with an immutable version or transformer
            firClass.replaceSuperTypeRefs(supertypeRefs)
        }
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): FirStatement {
        applyResolvedSupertypesToClass(anonymousObject)

        return anonymousObject.transformChildren(this, data) as FirAnonymousObject
    }

    private fun getResolvedSupertypeRefs(classLikeDeclaration: FirClassLikeDeclaration): List<FirResolvedTypeRef> {
        val status = supertypeComputationSession.getSupertypesComputationStatus(classLikeDeclaration)
        require(status is SupertypeComputationStatus.Computed) {
            "Unexpected status at FirApplySupertypesTransformer: $status for ${classLikeDeclaration.symbol.classId}"
        }
        return status.supertypeRefs
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Any?): FirStatement {
        if (typeAlias.expandedTypeRef is FirResolvedTypeRef) {
            return typeAlias
        }
        val supertypeRefs = getResolvedSupertypeRefs(typeAlias)

        assert(supertypeRefs.size == 1) {
            "Expected single supertypeRefs, but found ${supertypeRefs.size} in ${typeAlias.symbol.classId}"
        }

        // TODO: Replace with an immutable version or transformer
        typeAlias.replaceExpandedTypeRef(supertypeRefs[0])
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
        ).asReversed().mapNotNullTo(this) {
            it.lookupTag.getNestedClassifierScope(session, scopeSession)
                ?.wrapNestedClassifierScopeWithSubstitutionForSuperType(it, session)
        }
        // The type parameters scope has already been
        // added by this time;
        // See: prepareScopes()
    }

fun FirRegularClass.resolveSupertypesInTheAir(session: FirSession): List<FirTypeRef> {
    return FirSupertypeResolverVisitor(session, SupertypeComputationSession(), ScopeSession()).run {
        withFile(session.firProvider.getFirClassifierContainerFile(this@resolveSupertypesInTheAir.symbol)) {
            resolveSpecificClassLikeSupertypes(this@resolveSupertypesInTheAir, superTypeRefs)
        }
    }
}

open class FirSupertypeResolverVisitor(
    private val session: FirSession,
    private val supertypeComputationSession: SupertypeComputationSession,
    private val scopeSession: ScopeSession,
    private val scopeForLocalClass: PersistentList<FirScope>? = null,
    private val localClassesNavigationInfo: LocalClassesNavigationInfo? = null,
    private val firProviderInterceptor: FirProviderInterceptor? = null,
    @property:PrivateForInline var useSiteFile: FirFile? = null,
    containingDeclarations: List<FirDeclaration> = emptyList(),
) : FirDefaultVisitor<Unit, Any?>() {
    private val supertypeGenerationExtensions = session.extensionService.supertypeGenerators
    private val classDeclarationsStack = ArrayDeque<FirClass>()

    init {
        containingDeclarations.forEach {
            if (it is FirClass) {
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

    private fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? =
        if (firProviderInterceptor != null) firProviderInterceptor.getFirClassifierContainerFileIfAny(symbol)
        else session.firProvider.getFirClassifierContainerFileIfAny(symbol.classId)

    private fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? =
        if (firProviderInterceptor != null) firProviderInterceptor.getFirClassifierByFqName(classId)
        else session.firProvider.getFirClassifierByFqName(classId)

    override fun visitElement(element: FirElement, data: Any?) {}

    private fun prepareFileScopes(file: FirFile): ScopePersistentList {
        return supertypeComputationSession.getOrPutFileScope(file) {
            createImportingScopes(file, session, scopeSession).asReversed().toPersistentList()
        }
    }

    private fun prepareScopeForNestedClasses(klass: FirClass): ScopePersistentList {
        return supertypeComputationSession.getOrPutScopeForNestedClasses(klass) {
            calculateScopes(klass, true)
        }
    }

    private fun prepareScopeForCompanion(klass: FirClass): ScopePersistentList {
        return supertypeComputationSession.getOrPutScopeForCompanion(klass) {
            calculateScopes(klass, false)
        }
    }

    private fun calculateScopes(
        klass: FirClass,
        withCompanionScopes: Boolean,
    ): PersistentList<FirScope> {
        resolveAllSupertypes(klass, klass.superTypeRefs)
        return prepareScopes(klass).pushAll(
            createOtherScopesForNestedClassesOrCompanion(klass, session, scopeSession, supertypeComputationSession, withCompanionScopes)
        )
    }

    private fun resolveAllSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        supertypeRefs: List<FirTypeRef>,
        visited: MutableSet<FirClassLikeDeclaration> = mutableSetOf()
    ) {
        if (!visited.add(classLikeDeclaration)) return
        val supertypes =
            resolveSpecificClassLikeSupertypes(classLikeDeclaration, supertypeRefs).map { it.coneType }

        for (supertype in supertypes) {
            if (supertype !is ConeClassLikeType) continue
            val fir = getFirClassifierByFqName(supertype.lookupTag.classId) ?: continue
            resolveAllSupertypes(fir, fir.supertypeRefs(), visited)
        }
    }

    private fun FirClassLikeDeclaration.supertypeRefs() = when (this) {
        is FirRegularClass -> superTypeRefs
        is FirTypeAlias -> listOf(expandedTypeRef)
        else -> emptyList()
    }

    private fun prepareScopes(classLikeDeclaration: FirClassLikeDeclaration): PersistentList<FirScope> {
        val classId = classLikeDeclaration.symbol.classId

        val result = when {
            classId.isLocal -> {
                // Local classes should be treated specially and supplied with localClassesNavigationInfo, normally
                // But it seems to be too strict to add an assertion here
                if (localClassesNavigationInfo == null) return persistentListOf()

                val parent = localClassesNavigationInfo.parentForClass[classLikeDeclaration]

                when {
                    parent != null && parent is FirClass -> prepareScopeForNestedClasses(parent)
                    else -> scopeForLocalClass ?: return persistentListOf()
                }
            }
            classLikeDeclaration.safeAs<FirRegularClass>()?.isCompanion == true -> {
                val outerClassFir = classId.outerClassId?.let(::getFirClassifierByFqName) as? FirRegularClass
                prepareScopeForCompanion(outerClassFir ?: return persistentListOf())
            }
            classId.isNestedClass -> {
                val outerClassFir = classId.outerClassId?.let(::getFirClassifierByFqName) as? FirRegularClass
                prepareScopeForNestedClasses(outerClassFir ?: return persistentListOf())
            }
            else -> getFirClassifierContainerFileIfAny(classLikeDeclaration.symbol)?.let(::prepareFileScopes) ?: persistentListOf()
        }

        return result.pushIfNotNull(classLikeDeclaration.typeParametersScope())
    }

    private fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolveSuperTypeRefs: (FirTransformer<ScopeClassDeclaration>, ScopeClassDeclaration) -> List<FirResolvedTypeRef>
    ): List<FirTypeRef> {
        when (val status = supertypeComputationSession.getSupertypesComputationStatus(classLikeDeclaration)) {
            is SupertypeComputationStatus.Computed -> return status.supertypeRefs
            is SupertypeComputationStatus.Computing -> return listOf(
                createErrorTypeRef(
                    classLikeDeclaration,
                    "Loop in supertype definition for ${classLikeDeclaration.symbol.classId}",
                    if (classLikeDeclaration is FirTypeAlias) DiagnosticKind.RecursiveTypealiasExpansion else DiagnosticKind.LoopInSupertype
                )
            )
            SupertypeComputationStatus.NotComputed -> {}
        }

        supertypeComputationSession.startComputingSupertypes(classLikeDeclaration)
        val scopes = prepareScopes(classLikeDeclaration)

        val transformer = FirSpecificTypeResolverTransformer(session, supertypeSupplier = supertypeComputationSession.supertypesSupplier)

        @OptIn(PrivateForInline::class)
        val resolvedTypesRefs = transformer.withFile(useSiteFile) {
            resolveSuperTypeRefs(
                transformer,
                ScopeClassDeclaration(scopes, classDeclarationsStack)
            )
        }

        supertypeComputationSession.storeSupertypes(classLikeDeclaration, resolvedTypesRefs)
        return resolvedTypesRefs
    }

    open fun visitDeclarationContent(declaration: FirDeclaration, data: Any?) {
        declaration.acceptChildren(this, null)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: Any?) {
        withClassDeclarationCleanup(classDeclarationsStack, regularClass) {
            resolveSpecificClassLikeSupertypes(regularClass, regularClass.superTypeRefs)
            visitDeclarationContent(regularClass, null)
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?) {
        withClassDeclarationCleanup(classDeclarationsStack, anonymousObject) {
            resolveSpecificClassLikeSupertypes(anonymousObject, anonymousObject.superTypeRefs)
            visitDeclarationContent(anonymousObject, null)
        }
    }

    fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        supertypeRefs: List<FirTypeRef>
    ): List<FirTypeRef> {
        return resolveSpecificClassLikeSupertypes(classLikeDeclaration) { transformer, scopeDeclaration ->
            if (!classLikeDeclaration.isLocalClassOrAnonymousObject()) {
                session.lookupTracker?.let {
                    val fileSource = getFirClassifierContainerFileIfAny(classLikeDeclaration.symbol)?.source
                    for (supertypeRef in supertypeRefs) {
                        val scopeOwnerLookupNames = scopeDeclaration.scopes.flatMap { scope -> scope.scopeOwnerLookupNames }
                        it.recordTypeLookup(supertypeRef, scopeOwnerLookupNames, fileSource)
                    }
                }
            }
            /*
              This list is backed by mutable list and during iterating on it we can resolve supertypes of that class via IDE light classes
              as IJ Java resolve may resolve a lot of stuff by light classes
              this causes ConcurrentModificationException
              So we create a copy of supertypeRefs to avoid it
             */
            supertypeRefs.createCopy().mapTo(mutableListOf()) {
                val superTypeRef = it.transform<FirTypeRef, ScopeClassDeclaration>(transformer, scopeDeclaration)
                val typeParameterType = superTypeRef.coneTypeSafe<ConeTypeParameterType>()
                when {
                    typeParameterType != null ->
                        buildErrorTypeRef {
                            source = superTypeRef.source
                            diagnostic = ConeTypeParameterSupertype(typeParameterType.lookupTag.typeParameterSymbol)
                        }
                    superTypeRef !is FirResolvedTypeRef ->
                        createErrorTypeRef(
                            superTypeRef,
                            "Unresolved super-type: ${superTypeRef.render()}",
                            DiagnosticKind.UnresolvedSupertype
                        )
                    else ->
                        superTypeRef
                }
            }.also {
                addSupertypesFromExtensions(classLikeDeclaration, it)
            }
        }
    }

    private fun <T> List<T>.createCopy(): List<T> = ArrayList(this)

    private fun addSupertypesFromExtensions(klass: FirClassLikeDeclaration, supertypeRefs: MutableList<FirResolvedTypeRef>) {
        if (supertypeGenerationExtensions.isEmpty()) return
        for (extension in supertypeGenerationExtensions) {
            if (extension.needTransformSupertypes(klass)) {
                supertypeRefs += extension.computeAdditionalSupertypes(klass, supertypeRefs)
            }
        }
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Any?) {
        // TODO: this if is a temporary hack for built-in types (because we can't load file for them)
        if (typeAlias.expandedTypeRef is FirResolvedTypeRef) {
            return
        }

        resolveSpecificClassLikeSupertypes(typeAlias) { transformer, scope ->
            val resolvedTypeRef =
                transformer.transformTypeRef(typeAlias.expandedTypeRef, scope) as? FirResolvedTypeRef
                    ?: return@resolveSpecificClassLikeSupertypes listOf(
                        createErrorTypeRef(
                            typeAlias.expandedTypeRef,
                            "Unresolved expanded typeRef for ${typeAlias.symbol.classId}",
                            DiagnosticKind.UnresolvedExpandedType
                        )
                    )

            fun visitNestedTypeAliases(type: TypeArgumentMarker) {
                if (type is ConeClassLikeType) {
                    val symbol = type.lookupTag.toSymbol(session)
                    if (symbol is FirTypeAliasSymbol) {
                        visitTypeAlias(symbol.fir, null)
                    } else if (symbol is FirClassLikeSymbol) {
                        for (typeArgument in type.typeArguments) {
                            visitNestedTypeAliases(typeArgument)
                        }
                    }
                }
            }

            visitNestedTypeAliases(resolvedTypeRef.type)

            listOf(resolvedTypeRef)
        }
    }

    override fun visitFile(file: FirFile, data: Any?) {
        withFile(file) {
            visitDeclarationContent(file, null)
        }
    }
}

private fun createErrorTypeRef(fir: FirElement, message: String, kind: DiagnosticKind) = buildErrorTypeRef {
    source = fir.source
    diagnostic = ConeSimpleDiagnostic(message, kind)
}

class SupertypeComputationSession {
    private val fileScopesMap = hashMapOf<FirFile, ScopePersistentList>()
    private val scopesForNestedClassesMap = hashMapOf<FirClass, ScopePersistentList>()
    private val scopesForCompanionMap = hashMapOf<FirClass, ScopePersistentList>()
    val supertypeStatusMap = linkedMapOf<FirClassLikeDeclaration, SupertypeComputationStatus>()

    val supertypesSupplier: SupertypeSupplier = object : SupertypeSupplier() {
        override fun forClass(firClass: FirClass, useSiteSession: FirSession): List<ConeClassLikeType> {
            if (firClass.superTypeRefs.all { it is FirResolvedTypeRef }) return firClass.superConeTypes
            return (getSupertypesComputationStatus(firClass) as? SupertypeComputationStatus.Computed)?.supertypeRefs?.mapNotNull {
                it.coneTypeSafe<ConeClassLikeType>()
            }.orEmpty()
        }

        override fun expansionForTypeAlias(typeAlias: FirTypeAlias, useSiteSession: FirSession): ConeClassLikeType? {
            if (typeAlias.expandedTypeRef is FirResolvedTypeRef) return typeAlias.expandedConeType
            return (getSupertypesComputationStatus(typeAlias) as? SupertypeComputationStatus.Computed)
                ?.supertypeRefs
                ?.getOrNull(0)?.coneTypeSafe()
        }
    }

    fun getSupertypesComputationStatus(classLikeDeclaration: FirClassLikeDeclaration): SupertypeComputationStatus =
        supertypeStatusMap[classLikeDeclaration] ?: SupertypeComputationStatus.NotComputed

    fun getOrPutFileScope(file: FirFile, scope: () -> ScopePersistentList): ScopePersistentList =
        fileScopesMap.getOrPut(file) { scope() }

    fun getOrPutScopeForNestedClasses(klass: FirClass, scope: () -> ScopePersistentList): ScopePersistentList =
        scopesForNestedClassesMap.getOrPut(klass) { scope() }

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

    fun breakLoops(session: FirSession) {
        val visitedClassLikeDecls = mutableSetOf<FirClassLikeDeclaration>()
        val loopedClassLikeDecls = mutableSetOf<FirClassLikeDeclaration>()
        val path = mutableListOf<FirClassLikeDeclaration>()
        val pathSet = mutableSetOf<FirClassLikeDeclaration>()

        fun checkIsInLoop(classLikeDecl: FirClassLikeDeclaration?) {
            if (classLikeDecl == null) return

            val supertypeRefs: List<FirResolvedTypeRef>
            val supertypeComputationStatus = supertypeStatusMap[classLikeDecl]
            supertypeRefs = if (supertypeComputationStatus != null) {
                require(supertypeComputationStatus is SupertypeComputationStatus.Computed) {
                    "Expected computed supertypes in breakLoops for ${classLikeDecl.symbol.classId}"
                }
                supertypeComputationStatus.supertypeRefs
            } else {
                when (classLikeDecl) {
                    is FirRegularClass ->
                        classLikeDecl.superTypeRefs.filterIsInstance<FirResolvedTypeRef>()
                    is FirTypeAlias ->
                        (classLikeDecl.expandedTypeRef as? FirResolvedTypeRef)?.let { listOf(it) } ?: listOf()
                    else -> return
                }
            }

            if (classLikeDecl in visitedClassLikeDecls) {
                if (classLikeDecl in pathSet) {
                    loopedClassLikeDecls.add(classLikeDecl)
                    loopedClassLikeDecls.addAll(path.takeLastWhile { element -> element != classLikeDecl })
                }
                return
            }

            path.add(classLikeDecl)
            pathSet.add(classLikeDecl)
            visitedClassLikeDecls.add(classLikeDecl)

            val parentId = classLikeDecl.symbol.classId.relativeClassName.parent()
            if (!parentId.isRoot) {
                val parentSymbol = session.symbolProvider.getClassLikeSymbolByClassId(ClassId.fromString(parentId.asString()))
                if (parentSymbol is FirRegularClassSymbol) {
                    checkIsInLoop(parentSymbol.fir)
                }
            }

            val isTypeAlias = classLikeDecl is FirTypeAlias
            var isErrorInSupertypesFound = false
            val resultSupertypeRefs = mutableListOf<FirResolvedTypeRef>()
            for (supertypeRef in supertypeRefs) {
                val supertypeFir = supertypeRef.firClassLike(session)
                checkIsInLoop(supertypeFir)

                if (isTypeAlias) {
                    for (typeArgument in supertypeRef.type.typeArguments) {
                        if (typeArgument is ConeClassLikeType) {
                            checkIsInLoop(typeArgument.lookupTag.toSymbol(session)?.fir)
                        }
                    }
                }

                resultSupertypeRefs.add(
                    if (classLikeDecl in loopedClassLikeDecls) {
                        isErrorInSupertypesFound = true
                        createErrorTypeRef(
                            supertypeRef,
                            "Loop in supertype: ${classLikeDecl.symbol.classId} -> ${supertypeFir?.symbol?.classId}",
                            if (isTypeAlias) DiagnosticKind.RecursiveTypealiasExpansion else DiagnosticKind.LoopInSupertype
                        )
                    } else {
                        supertypeRef
                    }
                )
            }

            if (isErrorInSupertypesFound) {
                supertypeStatusMap[classLikeDecl] = SupertypeComputationStatus.Computed(resultSupertypeRefs)
            }

            path.removeAt(path.size - 1)
            pathSet.remove(classLikeDecl)
        }

        for (classifier in newClassifiersForBreakingLoops) {
            checkIsInLoop(classifier)
            require(path.isEmpty()) {
                "Path should be empty"
            }
        }
        newClassifiersForBreakingLoops.clear()
    }
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

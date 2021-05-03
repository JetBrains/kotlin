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
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.supertypeGenerators
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isLocalClassOrAnonymousObject
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeParameterSupertype
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.jetbrains.kotlin.fir.scopes.FirCompositeScope
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
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirSupertypeResolverProcessor(session: FirSession, scopeSession: ScopeSession) :
    FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = FirSupertypeResolverTransformer(session, scopeSession)
}

/**
 * Interceptor needed by IDE to resolve in-air created declarations.
 */
interface FirProviderInterceptorForSupertypeResolver {
    fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile?
    fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration<*>?
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

fun <F : FirClassLikeDeclaration<F>> F.runSupertypeResolvePhaseForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<FirScope>,
    localClassesNavigationInfo: LocalClassesNavigationInfo,
    firProviderInterceptor: FirProviderInterceptorForSupertypeResolver?,
): F {
    val supertypeComputationSession = SupertypeComputationSession()
    val supertypeResolverVisitor = FirSupertypeResolverVisitor(
        session, supertypeComputationSession, scopeSession,
        currentScopeList.toPersistentList(),
        localClassesNavigationInfo,
        firProviderInterceptor
    )

    this.accept(supertypeResolverVisitor, null)
    supertypeComputationSession.breakLoops(session)

    val applySupertypesTransformer = FirApplySupertypesTransformer(supertypeComputationSession)
    return this.transform<F, Nothing?>(applySupertypesTransformer, null)
}

class FirApplySupertypesTransformer(
    private val supertypeComputationSession: SupertypeComputationSession
) : FirDefaultTransformer<Any?>() {
    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Any?): FirDeclaration {
        file.replaceResolvePhase(FirResolvePhase.SUPER_TYPES)

        return (file.transformChildren(this, null) as FirFile)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): FirStatement {
        applyResolvedSupertypesToClass(regularClass)

        return (regularClass.transformChildren(this, null) as FirRegularClass)
    }

    private fun applyResolvedSupertypesToClass(firClass: FirClass<*>) {
        if (firClass.superTypeRefs.any { it !is FirResolvedTypeRef }) {
            val supertypeRefs = getResolvedSupertypeRefs(firClass)

            // TODO: Replace with an immutable version or transformer
            firClass.replaceSuperTypeRefs(supertypeRefs)
        }
        firClass.replaceResolvePhase(FirResolvePhase.SUPER_TYPES)
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?): FirStatement {
        applyResolvedSupertypesToClass(anonymousObject)

        return super.transformAnonymousObject(anonymousObject, data)
    }

    private fun getResolvedSupertypeRefs(classLikeDeclaration: FirClassLikeDeclaration<*>): List<FirResolvedTypeRef> {
        val status = supertypeComputationSession.getSupertypesComputationStatus(classLikeDeclaration)
        require(status is SupertypeComputationStatus.Computed) {
            "Unexpected status at FirApplySupertypesTransformer: $status for ${classLikeDeclaration.symbol.classId}"
        }
        return status.supertypeRefs
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Any?): FirDeclaration {
        if (typeAlias.expandedTypeRef is FirResolvedTypeRef) return typeAlias
        val supertypeRefs = getResolvedSupertypeRefs(typeAlias)

        assert(supertypeRefs.size == 1) {
            "Expected single supertypeRefs, but found ${supertypeRefs.size} in ${typeAlias.symbol.classId}"
        }

        // TODO: Replace with an immutable version or transformer
        typeAlias.replaceExpandedTypeRef(supertypeRefs[0])
        typeAlias.replaceResolvePhase(FirResolvePhase.SUPER_TYPES)

        return typeAlias
    }
}

private fun FirClassLikeDeclaration<*>.typeParametersScope(): FirScope? {
    if (this !is FirMemberDeclaration || typeParameters.isEmpty()) return null
    return FirMemberTypeParameterScope(this)
}

private fun createScopesForNestedClasses(
    klass: FirClass<*>,
    session: FirSession,
    scopeSession: ScopeSession,
    supertypeComputationSession: SupertypeComputationSession
): Collection<FirScope> =
    mutableListOf<FirScope>().apply {
        lookupSuperTypes(
            klass,
            lookupInterfaces = false, deep = true, substituteTypes = true, useSiteSession = session,
            supertypeSupplier = supertypeComputationSession.supertypesSupplier
        ).asReversed().mapNotNullTo(this) {
            it.lookupTag.getNestedClassifierScope(session, scopeSession)
                ?.wrapNestedClassifierScopeWithSubstitutionForSuperType(it, session)
        }
        addIfNotNull(klass.typeParametersScope())
        val companionObjects = klass.declarations.filterIsInstance<FirRegularClass>().filter { it.isCompanion }
        for (companionObject in companionObjects) {
            addIfNotNull(session.nestedClassifierScope(companionObject))
        }
        addIfNotNull(session.nestedClassifierScope(klass))
    }

fun FirRegularClass.resolveSupertypesInTheAir(session: FirSession): List<FirTypeRef> {
    return FirSupertypeResolverVisitor(session, SupertypeComputationSession(), ScopeSession())
        .resolveSpecificClassLikeSupertypes(this, superTypeRefs)
}

class FirSupertypeResolverVisitor(
    private val session: FirSession,
    private val supertypeComputationSession: SupertypeComputationSession,
    private val scopeSession: ScopeSession,
    private val scopeForLocalClass: PersistentList<FirScope>? = null,
    private val localClassesNavigationInfo: LocalClassesNavigationInfo? = null,
    private val firProviderInterceptor: FirProviderInterceptorForSupertypeResolver? = null,
) : FirDefaultVisitor<Unit, Any?>() {
    private val supertypeGenerationExtensions = session.extensionService.supertypeGenerators

    private fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? =
        if (firProviderInterceptor != null) firProviderInterceptor.getFirClassifierContainerFileIfAny(symbol)
        else session.firProvider.getFirClassifierContainerFileIfAny(symbol.classId)

    private fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration<*>? =
        if (firProviderInterceptor != null) firProviderInterceptor.getFirClassifierByFqName(classId)
        else session.firProvider.getFirClassifierByFqName(classId)

    override fun visitElement(element: FirElement, data: Any?) {}

    private fun prepareFileScopes(file: FirFile): ScopePersistentList {
        return supertypeComputationSession.getOrPutFileScope(file) {
            createImportingScopes(file, session, scopeSession).asReversed().toPersistentList()
        }
    }

    private fun prepareScopeForNestedClasses(klass: FirClass<*>): ScopePersistentList {
        return supertypeComputationSession.getOrPutScopeForNestedClasses(klass) {
            val scopes = prepareScopes(klass)

            resolveAllSupertypes(klass, klass.superTypeRefs)
            scopes.pushAll(createScopesForNestedClasses(klass, session, scopeSession, supertypeComputationSession))
        }
    }

    private fun resolveAllSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration<*>,
        supertypeRefs: List<FirTypeRef>,
        visited: MutableSet<FirClassLikeDeclaration<*>> = mutableSetOf()
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

    private fun FirClassLikeDeclaration<*>.supertypeRefs() = when (this) {
        is FirRegularClass -> superTypeRefs
        is FirTypeAlias -> listOf(expandedTypeRef)
        else -> emptyList()
    }

    private fun prepareScopes(classLikeDeclaration: FirClassLikeDeclaration<*>): PersistentList<FirScope> {
        val classId = classLikeDeclaration.symbol.classId

        val result = when {
            classId.isLocal -> {
                // Local classes should be treated specially and supplied with localClassesNavigationInfo, normally
                // But it seems to be too strict to add an assertion here
                if (localClassesNavigationInfo == null) return persistentListOf()

                val parent = localClassesNavigationInfo.parentForClass[classLikeDeclaration]

                when {
                    parent != null && parent is FirClass<*> -> prepareScopeForNestedClasses(parent)
                    else -> scopeForLocalClass ?: return persistentListOf()
                }
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
        classLikeDeclaration: FirClassLikeDeclaration<*>,
        resolveSuperTypeRefs: (FirTransformer<FirScope>, FirScope) -> List<FirResolvedTypeRef>
    ): List<FirTypeRef> {
        when (val status = supertypeComputationSession.getSupertypesComputationStatus(classLikeDeclaration)) {
            is SupertypeComputationStatus.Computed -> return status.supertypeRefs
            is SupertypeComputationStatus.Computing -> return listOf(
                createErrorTypeRef(classLikeDeclaration, "Loop in supertype definition for ${classLikeDeclaration.symbol.classId}")
            )
        }

        supertypeComputationSession.startComputingSupertypes(classLikeDeclaration)
        val scopes = prepareScopes(classLikeDeclaration)

        val transformer = FirSpecificTypeResolverTransformer(session)
        val resolvedTypesRefs = resolveSuperTypeRefs(transformer, FirCompositeScope(scopes))

        supertypeComputationSession.storeSupertypes(classLikeDeclaration, resolvedTypesRefs)
        return resolvedTypesRefs
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: Any?) {
        resolveSpecificClassLikeSupertypes(regularClass, regularClass.superTypeRefs)
        regularClass.acceptChildren(this, null)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Any?) {
        resolveSpecificClassLikeSupertypes(anonymousObject, anonymousObject.superTypeRefs)
        anonymousObject.acceptChildren(this, null)
    }

    fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration<*>,
        supertypeRefs: List<FirTypeRef>
    ): List<FirTypeRef> {
        return resolveSpecificClassLikeSupertypes(classLikeDeclaration) { transformer, scope ->
            if (!classLikeDeclaration.isLocalClassOrAnonymousObject()) {
                session.lookupTracker?.let {
                    val fileSource = getFirClassifierContainerFileIfAny(classLikeDeclaration.symbol)?.source
                    for (supertypeRef in supertypeRefs) {
                        it.recordTypeLookup(supertypeRef, scope.scopeOwnerLookupNames, fileSource)
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
                val superTypeRef = transformer.transformTypeRef(it, scope)
                val typeParameterType = superTypeRef.coneTypeSafe<ConeTypeParameterType>()
                when {
                    typeParameterType != null ->
                        buildErrorTypeRef {
                            source = superTypeRef.source
                            diagnostic = ConeTypeParameterSupertype(typeParameterType.lookupTag.typeParameterSymbol)
                        }
                    superTypeRef !is FirResolvedTypeRef ->
                        createErrorTypeRef(superTypeRef, "Unresolved super-type: ${superTypeRef.render()}")
                    else ->
                        superTypeRef
                }
            }.also {
                addSupertypesFromExtensions(classLikeDeclaration, it)
            }
        }
    }

    private fun <T> List<T>.createCopy(): List<T> = ArrayList(this)

    private fun addSupertypesFromExtensions(klass: FirClassLikeDeclaration<*>, supertypeRefs: MutableList<FirResolvedTypeRef>) {
        if (supertypeGenerationExtensions.isEmpty()) return
        val provider = session.predicateBasedProvider
        for (extension in supertypeGenerationExtensions) {
            if (provider.matches(extension.predicate, klass)) {
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
                            "Unresolved expanded typeRef for ${typeAlias.symbol.classId}"
                        )
                    )

            val type = resolvedTypeRef.type
            if (type is ConeClassLikeType) {
                val expansionTypeAlias = type.lookupTag.toSymbol(session)?.safeAs<FirTypeAliasSymbol>()?.fir
                if (expansionTypeAlias != null) {
                    visitTypeAlias(expansionTypeAlias, null)
                }
            }

            listOf(resolvedTypeRef)
        }
    }

    override fun visitFile(file: FirFile, data: Any?) {
        file.acceptChildren(this, null)
    }
}

private fun createErrorTypeRef(fir: FirElement, message: String) = buildErrorTypeRef {
    source = fir.source
    diagnostic = ConeSimpleDiagnostic(message)
}

class SupertypeComputationSession {
    private val fileScopesMap = hashMapOf<FirFile, ScopePersistentList>()
    private val scopesForNestedClassesMap = hashMapOf<FirClass<*>, ScopePersistentList>()
    private val supertypeStatusMap = linkedMapOf<FirClassLikeDeclaration<*>, SupertypeComputationStatus>()

    val supertypesSupplier: SupertypeSupplier = object : SupertypeSupplier() {
        override fun forClass(firClass: FirClass<*>, useSiteSession: FirSession): List<ConeClassLikeType> {
            if (firClass.resolvePhase > FirResolvePhase.SUPER_TYPES) return firClass.superConeTypes
            return (getSupertypesComputationStatus(firClass) as? SupertypeComputationStatus.Computed)?.supertypeRefs?.mapNotNull {
                it.coneTypeSafe<ConeClassLikeType>()
            }.orEmpty()
        }

        override fun expansionForTypeAlias(typeAlias: FirTypeAlias): ConeClassLikeType? {
            if (typeAlias.resolvePhase > FirResolvePhase.SUPER_TYPES) return typeAlias.expandedConeType
            return (getSupertypesComputationStatus(typeAlias) as? SupertypeComputationStatus.Computed)
                ?.supertypeRefs
                ?.getOrNull(0)?.coneTypeSafe()
        }
    }

    fun getSupertypesComputationStatus(classLikeDeclaration: FirClassLikeDeclaration<*>): SupertypeComputationStatus =
        supertypeStatusMap[classLikeDeclaration] ?: SupertypeComputationStatus.NotComputed

    fun getOrPutFileScope(file: FirFile, scope: () -> ScopePersistentList): ScopePersistentList =
        fileScopesMap.getOrPut(file) { scope() }

    fun getOrPutScopeForNestedClasses(klass: FirClass<*>, scope: () -> ScopePersistentList): ScopePersistentList =
        scopesForNestedClassesMap.getOrPut(klass) { scope() }

    fun startComputingSupertypes(classLikeDeclaration: FirClassLikeDeclaration<*>) {
        require(supertypeStatusMap[classLikeDeclaration] == null) {
            "Unexpected in startComputingSupertypes supertype status for $classLikeDeclaration: ${supertypeStatusMap[classLikeDeclaration]}"
        }

        supertypeStatusMap[classLikeDeclaration] = SupertypeComputationStatus.Computing
    }

    fun storeSupertypes(classLikeDeclaration: FirClassLikeDeclaration<*>, resolvedTypesRefs: List<FirResolvedTypeRef>) {
        require(supertypeStatusMap[classLikeDeclaration] is SupertypeComputationStatus.Computing) {
            "Unexpected in storeSupertypes supertype status for $classLikeDeclaration: ${supertypeStatusMap[classLikeDeclaration]}"
        }

        supertypeStatusMap[classLikeDeclaration] = SupertypeComputationStatus.Computed(resolvedTypesRefs)
        newClassifiersForBreakingLoops.add(classLikeDeclaration)
    }

    private val newClassifiersForBreakingLoops = mutableListOf<FirClassLikeDeclaration<*>>()
    private val breakLoopsDfsVisited = hashSetOf<FirClassLikeDeclaration<*>>()

    fun breakLoops(session: FirSession) {
        val inProcess = hashSetOf<FirClassLikeDeclaration<*>>()

        fun dfs(classLikeDeclaration: FirClassLikeDeclaration<*>) {
            if (classLikeDeclaration in breakLoopsDfsVisited) return
            val supertypeComputationStatus = supertypeStatusMap[classLikeDeclaration] ?: return
            if (classLikeDeclaration in inProcess) return

            inProcess.add(classLikeDeclaration)

            require(supertypeComputationStatus is SupertypeComputationStatus.Computed) {
                "Expected computed supertypes in breakLoops for ${classLikeDeclaration.symbol.classId}"
            }

            val typeRefs = supertypeComputationStatus.supertypeRefs
            val resultingTypeRefs = mutableListOf<FirResolvedTypeRef>()
            var wereChanges = false

            for (typeRef in typeRefs) {
                val fir = typeRef.firClassLike(session)
                fir?.let(::dfs)
                resultingTypeRefs.add(
                    if (fir in inProcess) {
                        wereChanges = true
                        createErrorTypeRef(
                            typeRef,
                            "Loop in supertype: ${classLikeDeclaration.symbol.classId} -> ${fir?.symbol?.classId}"
                        )
                    } else
                        typeRef
                )
            }

            if (wereChanges) {
                supertypeStatusMap[classLikeDeclaration] = SupertypeComputationStatus.Computed(resultingTypeRefs)
            }

            inProcess.remove(classLikeDeclaration)
            breakLoopsDfsVisited.add(classLikeDeclaration)
        }

        for (classifier in newClassifiersForBreakingLoops) {
            dfs(classifier)
        }
        newClassifiersForBreakingLoops.clear()
    }
}

fun FirTypeRef.firClassLike(session: FirSession): FirClassLikeDeclaration<*>? {
    val type = coneTypeSafe<ConeClassLikeType>() ?: return null
    return type.lookupTag.toSymbol(session)?.fir
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

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.supertypeGenerators
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.getNestedClassifierScope
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.jetbrains.kotlin.fir.scopes.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirSupertypeResolverProcessor(session: FirSession, scopeSession: ScopeSession) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = FirSupertypeResolverTransformer(session, scopeSession)
}

class FirSupertypeResolverTransformer(
    override val session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractPhaseTransformer<Nothing?>(FirResolvePhase.SUPER_TYPES) {
    private val supertypeComputationSession = SupertypeComputationSession()

    private val supertypeResolverVisitor = FirSupertypeResolverVisitor(session, supertypeComputationSession, scopeSession)
    private val applySupertypesTransformer = FirApplySupertypesTransformer(supertypeComputationSession)

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        checkSessionConsistency(file)
        file.accept(supertypeResolverVisitor)
        supertypeComputationSession.breakLoops(session)
        return file.transform(applySupertypesTransformer, null)
    }
}

fun <F : FirClass<F>> F.runSupertypeResolvePhaseForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<FirScope>,
    localClassesNavigationInfo: LocalClassesNavigationInfo,
): F {
    val supertypeComputationSession = SupertypeComputationSession()
    val supertypeResolverVisitor = FirSupertypeResolverVisitor(
        session, supertypeComputationSession, scopeSession,
        ImmutableList.ofAll(currentScopeList),
        localClassesNavigationInfo
    )

    this.accept(supertypeResolverVisitor)
    supertypeComputationSession.breakLoops(session)

    val applySupertypesTransformer = FirApplySupertypesTransformer(supertypeComputationSession)
    return this.transform<F, Nothing?>(applySupertypesTransformer, null).single
}

private class FirApplySupertypesTransformer(
    private val supertypeComputationSession: SupertypeComputationSession
) : FirDefaultTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        file.replaceResolvePhase(FirResolvePhase.SUPER_TYPES)

        return (file.transformChildren(this, null) as FirFile).compose()
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirStatement> {
        applyResolvedSupertypesToClass(regularClass)

        return (regularClass.transformChildren(this, null) as FirRegularClass).compose()
    }

    private fun applyResolvedSupertypesToClass(firClass: FirClass<*>) {
        if (firClass.superTypeRefs.any { it !is FirResolvedTypeRef }) {
            val supertypeRefs = getResolvedSupertypeRefs(firClass)

            // TODO: Replace with an immutable version or transformer
            firClass.replaceSuperTypeRefs(supertypeRefs)
            firClass.replaceResolvePhase(FirResolvePhase.SUPER_TYPES)
        }
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: Nothing?): CompositeTransformResult<FirStatement> {
        applyResolvedSupertypesToClass(anonymousObject)

        return super.transformAnonymousObject(anonymousObject, data)
    }

    private fun getResolvedSupertypeRefs(classLikeDeclaration: FirClassLikeDeclaration<*>): List<FirTypeRef> {
        val status = supertypeComputationSession.getSupertypesComputationStatus(classLikeDeclaration)
        require(status is SupertypeComputationStatus.Computed) {
            "Unexpected status at FirApplySupertypesTransformer: $status for ${classLikeDeclaration.symbol.classId}"
        }
        return status.supertypeRefs
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        if (typeAlias.expandedTypeRef is FirResolvedTypeRef) return typeAlias.compose()
        val supertypeRefs = getResolvedSupertypeRefs(typeAlias)

        assert(supertypeRefs.size == 1) {
            "Expected single supertypeRefs, but found ${supertypeRefs.size} in ${typeAlias.symbol.classId}"
        }

        // TODO: Replace with an immutable version or transformer
        typeAlias.replaceExpandedTypeRef(supertypeRefs[0])
        typeAlias.replaceResolvePhase(FirResolvePhase.SUPER_TYPES)

        return typeAlias.compose()
    }
}

private fun FirClassLikeDeclaration<*>.typeParametersScope(): FirScope? {
    if (this !is FirMemberDeclaration || typeParameters.isEmpty()) return null
    return FirMemberTypeParameterScope(this)
}

private fun createScopesForNestedClasses(
    klass: FirClass<*>,
    session: FirSession,
    supertypeComputationSession: SupertypeComputationSession
): Collection<FirScope> =
    mutableListOf<FirScope>().apply {
        lookupSuperTypes(
            klass,
            lookupInterfaces = false, deep = true, useSiteSession = session,
            supertypeSupplier = supertypeComputationSession.supertypesSupplier
        ).asReversed().mapNotNullTo(this) {
            session.getNestedClassifierScope(it.lookupTag)
        }
        addIfNotNull(klass.typeParametersScope())
        val companionObjects = klass.declarations.filterIsInstance<FirRegularClass>().filter { it.isCompanion }
        for (companionObject in companionObjects) {
            addIfNotNull(nestedClassifierScope(companionObject))
        }
        addIfNotNull(nestedClassifierScope(klass))
    }

fun FirRegularClass.resolveSupertypesInTheAir(session: FirSession): List<FirTypeRef> {
    return FirSupertypeResolverVisitor(session, SupertypeComputationSession(), ScopeSession())
        .resolveSpecificClassLikeSupertypes(this, superTypeRefs)
}

private class FirSupertypeResolverVisitor(
    private val session: FirSession,
    private val supertypeComputationSession: SupertypeComputationSession,
    private val scopeSession: ScopeSession,
    private val scopeForLocalClass: ImmutableList<FirScope>? = null,
    private val localClassesNavigationInfo: LocalClassesNavigationInfo? = null
) : FirDefaultVisitorVoid() {
    private val supertypeGenerationExtensions = session.extensionService.supertypeGenerators

    override fun visitElement(element: FirElement) {}

    private fun prepareFileScopes(file: FirFile): ScopeImmutableList {
        return supertypeComputationSession.getOrPutFileScope(file) {
            ImmutableList.ofAll(createImportingScopes(file, session, scopeSession).asReversed())
        }
    }

    private fun prepareScopeForNestedClasses(klass: FirClass<*>): ScopeImmutableList {
        return supertypeComputationSession.getOrPutScopeForNestedClasses(klass) {
            val scopes = prepareScopes(klass)

            resolveAllSupertypes(klass, klass.superTypeRefs)

            scopes.pushAll(createScopesForNestedClasses(klass, session, supertypeComputationSession))
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
            val fir = session.firProvider.getFirClassifierByFqName(supertype.lookupTag.classId) ?: continue
            resolveAllSupertypes(fir, fir.supertypeRefs(), visited)
        }
    }

    private fun FirClassLikeDeclaration<*>.supertypeRefs() = when (this) {
        is FirRegularClass -> superTypeRefs
        is FirTypeAlias -> listOf(expandedTypeRef)
        else -> emptyList()
    }

    private fun prepareScopes(classLikeDeclaration: FirClassLikeDeclaration<*>): ImmutableList<FirScope> {
        val classId = classLikeDeclaration.symbol.classId

        val result = when {
            classId.isLocal -> {
                // Local type aliases are not supported
                if (classLikeDeclaration !is FirClass<*>) return ImmutableList.empty()

                // Local classes should be treated specially and supplied with localClassesNavigationInfo, normally
                // But it seems to be too strict to add an assertion here
                val navigationInfo = localClassesNavigationInfo ?: return ImmutableList.empty()

                val parent = localClassesNavigationInfo.parentForClass[classLikeDeclaration]

                when {
                    parent != null -> prepareScopeForNestedClasses(parent)
                    else -> scopeForLocalClass ?: return ImmutableList.empty()
                }
            }
            classId.isNestedClass -> {
                val outerClassFir = classId.outerClassId?.let(session.firProvider::getFirClassifierByFqName) as? FirRegularClass
                prepareScopeForNestedClasses(outerClassFir ?: return ImmutableList.empty())
            }
            else -> prepareFileScopes(session.firProvider.getFirClassifierContainerFile(classId))
        }

        return result.pushIfNotNull(classLikeDeclaration.typeParametersScope())
    }

    private fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration<*>,
        resolveSuperTypeRefs: (FirTransformer<FirScope>, FirScope) -> List<FirTypeRef>
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

    override fun visitRegularClass(regularClass: FirRegularClass) {
        resolveSpecificClassLikeSupertypes(regularClass, regularClass.superTypeRefs)
        regularClass.acceptChildren(this)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
        resolveSpecificClassLikeSupertypes(anonymousObject, anonymousObject.superTypeRefs)
        anonymousObject.acceptChildren(this)
    }

    fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration<*>,
        supertypeRefs: List<FirTypeRef>
    ): List<FirTypeRef> {
        return resolveSpecificClassLikeSupertypes(classLikeDeclaration) { transformer, scope ->
            supertypeRefs.mapTo(mutableListOf()) {
                val superTypeRef = transformer.transformTypeRef(it, scope).single

                if (superTypeRef.coneTypeSafe<ConeTypeParameterType>() != null)
                    createErrorTypeRef(
                        superTypeRef,
                        "Type parameter cannot be a super-type: ${superTypeRef.coneTypeUnsafe<ConeTypeParameterType>().render()}"
                    )
                else
                    superTypeRef
            }.also {
                addSupertypesFromExtensions(classLikeDeclaration, it)
            }
        }
    }

    private fun addSupertypesFromExtensions(klass: FirClassLikeDeclaration<*>, supertypeRefs: MutableList<FirTypeRef>) {
        if (supertypeGenerationExtensions.isEmpty()) return
        val provider = session.predicateBasedProvider
        for (extension in supertypeGenerationExtensions) {
            if (provider.matches(extension.predicate, klass)) {
                supertypeRefs += extension.computeAdditionalSupertypes(klass, supertypeRefs)
            }
        }
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias) {
        // TODO: this if is a temporary hack for built-in types (because we can't load file for them)
        if (typeAlias.expandedTypeRef is FirResolvedTypeRef) {
            return
        }

        resolveSpecificClassLikeSupertypes(typeAlias) { transformer, scope ->
            val resolvedTypeRef =
                transformer.transformTypeRef(typeAlias.expandedTypeRef, scope).single as? FirResolvedTypeRef
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
                    visitTypeAlias(expansionTypeAlias)
                }
            }

            listOf(resolvedTypeRef)
        }
    }

    override fun visitFile(file: FirFile) {
        file.acceptChildren(this)
    }
}

private fun createErrorTypeRef(fir: FirElement, message: String) = buildErrorTypeRef {
    source = fir.source
    diagnostic = ConeSimpleDiagnostic(message)
}

private class SupertypeComputationSession {
    private val fileScopesMap = hashMapOf<FirFile, ScopeImmutableList>()
    private val scopesForNestedClassesMap = hashMapOf<FirClass<*>, ScopeImmutableList>()
    private val supertypeStatusMap = linkedMapOf<FirClassLikeDeclaration<*>, SupertypeComputationStatus>()

    val supertypesSupplier: SupertypeSupplier = object : SupertypeSupplier() {
        override fun forClass(firClass: FirClass<*>): List<ConeClassLikeType> {
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

    fun getOrPutFileScope(file: FirFile, scope: () -> ScopeImmutableList): ScopeImmutableList =
        fileScopesMap.getOrPut(file) { scope() }

    fun getOrPutScopeForNestedClasses(klass: FirClass<*>, scope: () -> ScopeImmutableList): ScopeImmutableList =
        scopesForNestedClassesMap.getOrPut(klass) { scope() }

    fun startComputingSupertypes(classLikeDeclaration: FirClassLikeDeclaration<*>) {
        require(supertypeStatusMap[classLikeDeclaration] == null) {
            "Unexpected in startComputingSupertypes supertype status for $classLikeDeclaration: ${supertypeStatusMap[classLikeDeclaration]}"
        }

        supertypeStatusMap[classLikeDeclaration] = SupertypeComputationStatus.Computing
    }

    fun storeSupertypes(classLikeDeclaration: FirClassLikeDeclaration<*>, resolvedTypesRefs: List<FirTypeRef>) {
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
            val resultingTypeRefs = mutableListOf<FirTypeRef>()
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

    class Computed(val supertypeRefs: List<FirTypeRef>) : SupertypeComputationStatus()
}

private typealias ImmutableList<E> = javaslang.collection.List<E>
private typealias ScopeImmutableList = ImmutableList<FirScope>

private fun ScopeImmutableList.pushIfNotNull(scope: FirScope?): ScopeImmutableList = if (scope == null) this else push(scope)

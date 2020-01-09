/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirSupertypeResolverTransformer : FirTransformer<Nothing?>() {
    private val supertypeComputationSession = SupertypeComputationSession()
    private val scopeSession = ScopeSession()
    private val applySupertypesTransformer = FirApplySupertypesTransformer(supertypeComputationSession)

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val supertypeResolverVisitor = FirSupertypeResolverVisitor(file.session, supertypeComputationSession, scopeSession)
        file.accept(supertypeResolverVisitor)
        supertypeComputationSession.breakLoops(file.session)
        return file.transform(applySupertypesTransformer, null)
    }
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
        if (regularClass.superTypeRefs.any { it !is FirResolvedTypeRef }) {
            val supertypeRefs = getResolvedSupertypeRefs(regularClass)

            // TODO: Replace with an immutable version or transformer
            regularClass.replaceSuperTypeRefs(supertypeRefs)
            regularClass.replaceResolvePhase(FirResolvePhase.SUPER_TYPES)
        }

        return (regularClass.transformChildren(this, null) as FirRegularClass).compose()
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
    regularClass: FirRegularClass,
    session: FirSession,
    supertypeComputationSession: SupertypeComputationSession
): Collection<FirScope> =
    mutableListOf<FirScope>().apply {
        lookupSuperTypes(
            regularClass,
            lookupInterfaces = false, deep = true, useSiteSession = session,
            supertypeSupplier = supertypeComputationSession.supertypesSupplier
        ).asReversed().mapNotNullTo(this) {
            session.firSymbolProvider.getNestedClassifierScope(it.lookupTag.classId)
        }
        addIfNotNull(regularClass.typeParametersScope())
        val companionObjects = regularClass.declarations.filterIsInstance<FirRegularClass>().filter { it.isCompanion }
        for (companionObject in companionObjects) {
            add(nestedClassifierScope(companionObject))
        }
        add(nestedClassifierScope(regularClass))
    }

fun FirRegularClass.resolveSupertypesInTheAir(session: FirSession): List<FirTypeRef> {
    return FirSupertypeResolverVisitor(session, SupertypeComputationSession(), ScopeSession())
        .resolveSpecificClassLikeSupertypes(this, superTypeRefs)
}

private class FirSupertypeResolverVisitor(
    private val session: FirSession,
    private val supertypeComputationSession: SupertypeComputationSession,
    private val scopeSession: ScopeSession
) : FirDefaultVisitorVoid() {
    override fun visitElement(element: FirElement) {}

    private fun prepareFileScope(file: FirFile): FirImmutableCompositeScope {
        return supertypeComputationSession.getOrPutFileScope(file) {
            FirImmutableCompositeScope(ImmutableList.ofAll(createImportingScopes(file, session, scopeSession).asReversed()))
        }
    }

    private fun prepareScopeForNestedClasses(regularClass: FirRegularClass): FirImmutableCompositeScope {
        return supertypeComputationSession.getOrPutScopeForNestedClasses(regularClass) {
            val scope = prepareScope(regularClass)

            resolveAllSupertypes(regularClass, regularClass.superTypeRefs)

            scope.childScope(createScopesForNestedClasses(regularClass, session, supertypeComputationSession))
        }
    }

    private fun resolveAllSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration<*>,
        supertypeRefs: List<FirTypeRef>,
        visited: MutableSet<FirClassLikeDeclaration<*>> = mutableSetOf()
    ) {
        if (!visited.add(classLikeDeclaration)) return
        val supertypes =
            resolveSpecificClassLikeSupertypes(classLikeDeclaration, supertypeRefs)
                .mapNotNull { (it as? FirResolvedTypeRef)?.type }

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

    private fun prepareScope(classLikeDeclaration: FirClassLikeDeclaration<*>): FirImmutableCompositeScope {
        val classId = classLikeDeclaration.symbol.classId
        val outerClassFir = classId.outerClassId?.let(session.firProvider::getFirClassifierByFqName) as? FirRegularClass

        val result = if (outerClassFir == null) {
            prepareFileScope(session.firProvider.getFirClassifierContainerFile(classId))
        } else {
            prepareScopeForNestedClasses(outerClassFir)
        }

        return result.childScope(classLikeDeclaration.typeParametersScope())
    }

    private fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration<*>,
        resolveSuperTypeRefs: (FirTransformer<Nothing?>) -> List<FirTypeRef>
    ): List<FirTypeRef> {
        when (val status = supertypeComputationSession.getSupertypesComputationStatus(classLikeDeclaration)) {
            is SupertypeComputationStatus.Computed -> return status.supertypeRefs
            is SupertypeComputationStatus.Computing -> return listOf(
                createErrorTypeRef(classLikeDeclaration, "Loop in supertype definition for ${classLikeDeclaration.symbol.classId}")
            )
        }

        supertypeComputationSession.startComputingSupertypes(classLikeDeclaration)
        val scope = prepareScope(classLikeDeclaration)

        val transformer = FirSpecificTypeResolverTransformer(scope, session)
        val resolvedTypesRefs = resolveSuperTypeRefs(transformer)

        supertypeComputationSession.storeSupertypes(classLikeDeclaration, resolvedTypesRefs)
        return resolvedTypesRefs
    }

    override fun visitRegularClass(regularClass: FirRegularClass) {
        resolveSpecificClassLikeSupertypes(regularClass, regularClass.superTypeRefs)
        regularClass.acceptChildren(this)
    }

    fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration<*>,
        supertypeRefs: List<FirTypeRef>
    ): List<FirTypeRef> {
        // TODO: this if is a temporary hack for built-in types (because we can't load file for them)
        if (supertypeRefs.all { it is FirResolvedTypeRef }) {
            return supertypeRefs
        }

        return resolveSpecificClassLikeSupertypes(classLikeDeclaration) { transformer ->
            supertypeRefs.map {
                val superTypeRef = transformer.transformTypeRef(it, null).single

                if (superTypeRef.coneTypeSafe<ConeTypeParameterType>() != null)
                    createErrorTypeRef(
                        superTypeRef,
                        "Type parameter cannot be a super-type: ${superTypeRef.coneTypeUnsafe<ConeTypeParameterType>().render()}"
                    )
                else
                    superTypeRef
            }
        }
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias) {
        // TODO: this if is a temporary hack for built-in types (because we can't load file for them)
        if (typeAlias.expandedTypeRef is FirResolvedTypeRef) {
            return
        }

        resolveSpecificClassLikeSupertypes(typeAlias) { transformer ->
            val resolvedTypeRef =
                transformer.transformTypeRef(typeAlias.expandedTypeRef, null).single as? FirResolvedTypeRef
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

private fun createErrorTypeRef(fir: FirElement, message: String) = FirErrorTypeRefImpl(fir.source, FirSimpleDiagnostic(message))

private class SupertypeComputationSession {
    private val fileScopesMap = hashMapOf<FirFile, FirImmutableCompositeScope>()
    private val scopesForNestedClassesMap = hashMapOf<FirRegularClass, FirImmutableCompositeScope>()
    private val supertypeStatusMap = linkedMapOf<FirClassLikeDeclaration<*>, SupertypeComputationStatus>()

    val supertypesSupplier = object : SupertypeSupplier() {
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

    fun getOrPutFileScope(file: FirFile, scope: () -> FirImmutableCompositeScope): FirImmutableCompositeScope =
        fileScopesMap.getOrPut(file) { scope() }

    fun getOrPutScopeForNestedClasses(regularClass: FirRegularClass, scope: () -> FirImmutableCompositeScope): FirImmutableCompositeScope =
        scopesForNestedClassesMap.getOrPut(regularClass) { scope() }

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
    }

    fun breakLoops(session: FirSession) {
        val visited = hashSetOf<FirClassLikeDeclaration<*>>()
        val inProcess = hashSetOf<FirClassLikeDeclaration<*>>()

        fun dfs(classLikeDeclaration: FirClassLikeDeclaration<*>) {
            val supertypeComputationStatus = supertypeStatusMap[classLikeDeclaration] ?: return
            if (classLikeDeclaration in visited) return
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
            visited.add(classLikeDeclaration)
        }

        for (classifier in supertypeStatusMap.keys) {
            dfs(classifier)
        }
    }
}

fun FirTypeRef.firClassLike(session: FirSession): FirClassLikeDeclaration<*>? {
    val type = coneTypeSafe<ConeClassLikeType>() ?: return null
    return session.firProvider.getFirClassifierByFqName(type.lookupTag.classId)
}

sealed class SupertypeComputationStatus {
    object NotComputed : SupertypeComputationStatus()
    object Computing : SupertypeComputationStatus()

    class Computed(val supertypeRefs: List<FirTypeRef>) : SupertypeComputationStatus()
}

private typealias ImmutableList<E> = javaslang.collection.List<E>

private class FirImmutableCompositeScope(
    private val scopes: ImmutableList<FirScope>
) : FirScope() {

    fun childScope(newScope: FirScope?) = newScope?.let { FirImmutableCompositeScope(scopes.push(newScope)) } ?: this
    fun childScope(newScopes: Collection<FirScope>) = FirImmutableCompositeScope(scopes.pushAll(newScopes))

    override fun processClassifiersByName(
        name: Name,
        processor: (FirClassifierSymbol<*>) -> Unit
    ) {
        for (scope in scopes) {
            scope.processClassifiersByName(name, processor)
        }
    }

    private inline fun <T> processComposite(
        process: FirScope.(Name, (T) -> Unit) -> Unit,
        name: Name,
        noinline processor: (T) -> Unit
    ) {
        val unique = mutableSetOf<T>()
        for (scope in scopes) {
            scope.process(name) {
                if (unique.add(it)) {
                    processor(it)
                }
            }
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        return processComposite(FirScope::processFunctionsByName, name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> Unit) {
        return processComposite(FirScope::processPropertiesByName, name, processor)
    }
}

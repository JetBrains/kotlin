/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.dfa.symbol
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.jetbrains.kotlin.fir.scopes.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle

@OptIn(AdapterForResolveProcessor::class)
class FirStatusResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = run {
        val statusComputationSession = StatusComputationSession.Regular()
        FirStatusResolveTransformer(
            session,
            scopeSession,
            statusComputationSession
        )
    }
}

fun <F : FirClass<F>> F.runStatusResolveForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    scopesForLocalClass: List<FirScope>,
    localClassesNavigationInfo: LocalClassesNavigationInfo
): F {
    val statusComputationSession = StatusComputationSession.ForLocalClassResolution(localClassesNavigationInfo.parentForClass.keys)
    val transformer = FirStatusResolveTransformer(
        session,
        scopeSession,
        statusComputationSession,
        localClassesNavigationInfo.parentForClass,
        FirCompositeScope(scopesForLocalClass)
    )

    return this.transform<F, Nothing?>(transformer, null).single
}

abstract class ResolvedStatusCalculator {
    abstract fun tryCalculateResolvedStatus(declaration: FirCallableMemberDeclaration<*>): FirResolvedDeclarationStatus

    object Default : ResolvedStatusCalculator() {
        override fun tryCalculateResolvedStatus(declaration: FirCallableMemberDeclaration<*>): FirResolvedDeclarationStatus {
            val status = declaration.status
            require(status is FirResolvedDeclarationStatus) {
                "Status of ${declaration.render()} is unresolved"
            }
            return status
        }
    }
}

class FirStatusResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    statusComputationSession: StatusComputationSession,
    designationMapForLocalClasses: Map<FirClass<*>, FirClass<*>?> = mapOf(),
    scopeForLocalClass: FirScope? = null,
) : AbstractFirStatusResolveTransformer(session, scopeSession, statusComputationSession, designationMapForLocalClasses, scopeForLocalClass) {
    override fun FirDeclaration.needResolveMembers(): Boolean {
        if (this is FirRegularClass) {
            return statusComputationSession[this] != StatusComputationSession.StatusComputationStatus.Computed
        }
        return true
    }

    override fun FirDeclaration.needResolveNestedClassifiers(): Boolean {
        return true
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        val computationStatus = statusComputationSession.startComputing(regularClass)
        forceResolveStatusesOfSupertypes(regularClass)
        /*
         * Status of class may be already calculated if that class was in supertypes of one of previous classes
         */
        if (computationStatus != StatusComputationSession.StatusComputationStatus.Computed) {
            regularClass.transformStatus(this, statusResolver.resolveStatus(regularClass, containingClass, isLocal = false))
        }
        return transformClass(regularClass, data).also {
            statusComputationSession.endComputing(regularClass)
        }
    }
}

private class FirDesignatedStatusResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    private val designation: Iterator<FirDeclaration>,
    private val targetClass: FirClass<*>,
    statusComputationSession: StatusComputationSession,
    designationMapForLocalClasses: Map<FirClass<*>, FirClass<*>?>,
    scopeForLocalClass: FirScope?,
) : AbstractFirStatusResolveTransformer(session, scopeSession, statusComputationSession, designationMapForLocalClasses, scopeForLocalClass) {
    private var currentElement: FirDeclaration? = null
    private var classLocated = false

    private fun shouldSkipClass(declaration: FirDeclaration): Boolean {
        if (classLocated) return declaration != targetClass
        if (currentElement == null && designation.hasNext()) {
            currentElement = designation.next()
        }
        val result = currentElement == declaration
        if (result) {
            if (currentElement == targetClass) {
                classLocated = true
            }
            currentElement = null
        }
        return false
    }

    override fun FirDeclaration.needResolveMembers(): Boolean {
        return classLocated
    }

    override fun FirDeclaration.needResolveNestedClassifiers(): Boolean {
        return !classLocated
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        if (shouldSkipClass(regularClass)) return regularClass.compose()
        regularClass.symbol.ensureResolved(FirResolvePhase.TYPES, session)
        val classLocated = this.classLocated
        /*
         * In designated status resolve we should resolve status only of target class and it's members
         */
        if (classLocated) {
            assert(regularClass == targetClass)
            val computationStatus = statusComputationSession.startComputing(regularClass)
            forceResolveStatusesOfSupertypes(regularClass)
            if (computationStatus != StatusComputationSession.StatusComputationStatus.Computed) {
                updateResolvePhaseOfMembers(regularClass)
                regularClass.transformStatus(this, statusResolver.resolveStatus(regularClass, containingClass, isLocal = false))
            }
        }
        return transformClass(regularClass, data).also {
            if (classLocated) statusComputationSession.endComputing(regularClass)
        }
    }
}

sealed class StatusComputationSession {
    abstract operator fun get(klass: FirClass<*>): StatusComputationStatus

    abstract fun startComputing(klass: FirClass<*>): StatusComputationStatus
    abstract fun endComputing(klass: FirClass<*>)

    enum class StatusComputationStatus {
        NotComputed, Computing, Computed
    }

    class Regular : StatusComputationSession() {
        private val statusMap = mutableMapOf<FirClass<*>, StatusComputationStatus>()
            .withDefault { StatusComputationStatus.NotComputed }

        override fun get(klass: FirClass<*>): StatusComputationStatus = statusMap.getValue(klass)

        override fun startComputing(klass: FirClass<*>): StatusComputationStatus {
            return statusMap.getOrPut(klass) { StatusComputationStatus.Computing }
        }

        override fun endComputing(klass: FirClass<*>) {
            statusMap[klass] = StatusComputationStatus.Computed
        }
    }

    class ForLocalClassResolution(private val localClasses: Set<FirClass<*>>) : StatusComputationSession() {
        private val delegate = Regular()

        override fun get(klass: FirClass<*>): StatusComputationStatus {
            if (klass !in localClasses) return StatusComputationStatus.Computed
            return delegate[klass]
        }

        override fun startComputing(klass: FirClass<*>): StatusComputationStatus {
            return delegate.startComputing(klass)
        }

        override fun endComputing(klass: FirClass<*>) {
            delegate.endComputing(klass)
        }
    }
}

abstract class AbstractFirStatusResolveTransformer(
    final override val session: FirSession,
    val scopeSession: ScopeSession,
    protected val statusComputationSession: StatusComputationSession,
    protected val designationMapForLocalClasses: Map<FirClass<*>, FirClass<*>?>,
    private val scopeForLocalClass: FirScope?
) : FirAbstractTreeTransformer<FirResolvedDeclarationStatus?>(phase = FirResolvePhase.STATUS) {
    private val classes = mutableListOf<FirClass<*>>()
    protected val statusResolver = FirStatusResolver(session, scopeSession)

    protected val containingClass: FirClass<*>? get() = classes.lastOrNull()

    private val firProvider = session.firProvider
    private val symbolProvider = session.firSymbolProvider

    protected abstract fun FirDeclaration.needResolveMembers(): Boolean
    protected abstract fun FirDeclaration.needResolveNestedClassifiers(): Boolean

    override fun transformFile(file: FirFile, data: FirResolvedDeclarationStatus?): CompositeTransformResult<FirFile> {
        file.replaceResolvePhase(transformerPhase)
        if (file.needResolveMembers()) {
            for (declaration in file.declarations) {
                if (declaration !is FirClassLikeDeclaration<*>) {
                    declaration.transformSingle(this, data)
                }
            }
        }
        if (file.needResolveNestedClassifiers()) {
            for (declaration in file.declarations) {
                if (declaration is FirClassLikeDeclaration<*>) {
                    declaration.transformSingle(this, data)
                }
            }
        }
        return file.compose()
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirDeclarationStatus> {
        return (data ?: declarationStatus).compose()
    }

    private inline fun storeClass(
        klass: FirClass<*>,
        computeResult: () -> CompositeTransformResult<FirDeclaration>
    ): CompositeTransformResult<FirDeclaration> {
        classes += klass
        val result = computeResult()
        classes.removeAt(classes.lastIndex)
        return result
    }

    override fun transformDeclaration(
        declaration: FirDeclaration,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        declaration.replaceResolvePhase(transformerPhase)
        return when (declaration) {
            is FirCallableDeclaration<*> -> {
                when (declaration) {
                    is FirProperty -> {
                        declaration.getter?.let { transformPropertyAccessor(it, data) }
                        declaration.setter?.let { transformPropertyAccessor(it, data) }
                    }
                    is FirFunction<*> -> {
                        for (valueParameter in declaration.valueParameters) {
                            transformValueParameter(valueParameter, data)
                        }
                    }
                }
                declaration.compose()
            }
            is FirPropertyAccessor -> {
                declaration.compose()
            }
            else -> {
                transformElement(declaration, data)
            }
        }
    }

    override fun transformTypeAlias(
        typeAlias: FirTypeAlias,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        typeAlias.typeParameters.forEach { transformDeclaration(it, data) }
        typeAlias.transformStatus(this, statusResolver.resolveStatus(typeAlias, containingClass, isLocal = false))
        return transformDeclaration(typeAlias, data)
    }

    abstract override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirStatement>

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        @Suppress("UNCHECKED_CAST")
        return transformClass(anonymousObject, data)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <F : FirClass<F>> transformClass(
        klass: FirClass<F>,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        return storeClass(klass) {
            klass.typeParameters.forEach { it.transformSingle(this, data) }
            klass.replaceResolvePhase(transformerPhase)
            if (klass.needResolveMembers()) {
                val members = klass.declarations.filter { it !is FirClassLikeDeclaration<*> }
                members.forEach { it.replaceResolvePhase(transformerPhase) }
                members.forEach { it.transformSingle(this, data) }
            }
            if (klass.needResolveNestedClassifiers()) {
                for (declaration in klass.declarations) {
                    if (declaration is FirClassLikeDeclaration<*>) {
                        declaration.transformSingle(this, data)
                    }
                }
            }
            klass.compose()
        } as CompositeTransformResult<FirStatement>
    }

    protected fun updateResolvePhaseOfMembers(regularClass: FirRegularClass) {
        for (declaration in regularClass.declarations) {
            if (declaration is FirProperty || declaration is FirSimpleFunction) {
                declaration.replaceResolvePhase(transformerPhase)
            }
        }
    }

    protected fun forceResolveStatusesOfSupertypes(regularClass: FirRegularClass) {
        for (superTypeRef in regularClass.superTypeRefs) {
            forceResolveStatusOfCorrespondingClass(superTypeRef, regularClass)
        }
    }

    private fun forceResolveStatusOfCorrespondingClass(typeRef: FirTypeRef, subClass: FirRegularClass?) {
        val classId = typeRef.coneType.classId ?: return
        val superClass = when {
            classId.isLocal -> {
                requireNotNull(subClass)
                var parent = designationMapForLocalClasses[subClass] as? FirRegularClass
                if (parent == null && scopeForLocalClass != null) {
                    scopeForLocalClass.processClassifiersByName(classId.shortClassName) {
                        if (it is FirRegularClass && it.classId == classId) {
                            parent = it
                        }
                    }
                }
                parent
            }
            else -> symbolProvider.getClassLikeSymbolByFqName(classId)?.fir
        } ?: return
        when (superClass) {
            is FirRegularClass -> forceResolveStatusesOfClass(superClass)
            is FirTypeAlias -> forceResolveStatusOfCorrespondingClass(superClass.expandedTypeRef, subClass = null)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun forceResolveStatusesOfClass(regularClass: FirRegularClass) {
        if (statusComputationSession[regularClass] == StatusComputationSession.StatusComputationStatus.Computed) return
        val file = firProvider.getFirClassifierContainerFileIfAny(regularClass.symbol)
        if (regularClass.status is FirResolvedDeclarationStatus) {
            statusComputationSession.endComputing(regularClass)
            /*
             * If regular class has no corresponding file then it is platform class,
             *   so we need to resolve supertypes of this class because they could
             *   come from kotlin sources
             */
            if (file == null) {
                forceResolveStatusesOfSupertypes(regularClass)
            }
            return
        }
        require(file != null) {
            "Containing file was not found for\n${regularClass.render()}"
        }
        val symbol = regularClass.symbol
        var declarationToStart: FirDeclaration? = null
        val designation = designationMapForLocalClasses[regularClass]?.let(::listOf)?.also {
            declarationToStart = it.first()
        } ?: buildList<FirDeclaration> {
            val outerClasses = generateSequence(symbol.classId) { classId ->
                classId.outerClassId
            }.mapTo(mutableListOf()) { firProvider.getFirClassifierByFqName(it) }
            declarationToStart = file
            this += outerClasses.filterNotNull().asReversed()
        }
        if (designation.isEmpty()) return

        val transformer = FirDesignatedStatusResolveTransformer(
            session,
            scopeSession,
            designation.iterator(),
            regularClass,
            statusComputationSession,
            designationMapForLocalClasses,
            scopeForLocalClass
        )
        declarationToStart!!.transformSingle(transformer, null)
        statusComputationSession.endComputing(regularClass)
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        propertyAccessor.transformStatus(this, statusResolver.resolveStatus(propertyAccessor, containingClass, isLocal = false))
        @Suppress("UNCHECKED_CAST")
        return transformDeclaration(propertyAccessor, data)
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        constructor.transformStatus(this, statusResolver.resolveStatus(constructor, containingClass, isLocal = false))
        return transformDeclaration(constructor, data)
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        simpleFunction.replaceResolvePhase(transformerPhase)
        simpleFunction.transformStatus(this, statusResolver.resolveStatus(simpleFunction, containingClass, isLocal = false))
        return transformDeclaration(simpleFunction, data)
    }

    override fun transformProperty(
        property: FirProperty,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        property.replaceResolvePhase(transformerPhase)
        property.transformStatus(this, statusResolver.resolveStatus(property, containingClass, isLocal = false))
        return transformDeclaration(property, data)
    }

    override fun transformField(
        field: FirField,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        field.transformStatus(this, statusResolver.resolveStatus(field, containingClass, isLocal = false))
        return transformDeclaration(field, data)
    }

    override fun transformEnumEntry(
        enumEntry: FirEnumEntry,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        enumEntry.transformStatus(this, statusResolver.resolveStatus(enumEntry, containingClass, isLocal = false))
        return transformDeclaration(enumEntry, data)
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        @Suppress("UNCHECKED_CAST")
        return transformDeclaration(valueParameter, data) as CompositeTransformResult<FirStatement>
    }

    override fun transformTypeParameter(
        typeParameter: FirTypeParameter,
        data: FirResolvedDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(typeParameter, data)
    }

    override fun transformBlock(block: FirBlock, data: FirResolvedDeclarationStatus?): CompositeTransformResult<FirStatement> {
        return block.compose()
    }
}

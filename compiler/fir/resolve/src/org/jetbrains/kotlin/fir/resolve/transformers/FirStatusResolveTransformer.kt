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
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.jetbrains.kotlin.fir.scopes.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toSymbol
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

fun <F : FirClassLikeDeclaration<F>> F.runStatusResolveForLocalClass(
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

    return this.transform(transformer, null)
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
    designationMapForLocalClasses: Map<FirClassLikeDeclaration<*>, FirClassLikeDeclaration<*>?> = mapOf(),
    scopeForLocalClass: FirScope? = null,
) : AbstractFirStatusResolveTransformer(
    session,
    scopeSession,
    statusComputationSession,
    designationMapForLocalClasses,
    scopeForLocalClass
) {
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
    ): FirStatement {
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

open class FirDesignatedStatusResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    private val designation: Iterator<FirDeclaration>,
    private val targetClass: FirClassLikeDeclaration<*>,
    statusComputationSession: StatusComputationSession,
    designationMapForLocalClasses: Map<FirClassLikeDeclaration<*>, FirClassLikeDeclaration<*>?>,
    scopeForLocalClass: FirScope?,
) : AbstractFirStatusResolveTransformer(
    session,
    scopeSession,
    statusComputationSession,
    designationMapForLocalClasses,
    scopeForLocalClass
) {
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
        return !result
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
    ): FirStatement {
        if (shouldSkipClass(regularClass)) return regularClass
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
        } else {
            if (regularClass.status !is FirResolvedDeclarationStatus) {
                regularClass.transformStatus(this, statusResolver.resolveStatus(regularClass, containingClass, isLocal = false))
                statusComputationSession.computeOnlyClassStatus(regularClass)
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
    abstract fun computeOnlyClassStatus(klass: FirClass<*>)

    enum class StatusComputationStatus(val requiresComputation: Boolean) {
        NotComputed(true),
        Computing(false),
        ComputedOnlyClassStatus(true),
        Computed(false)
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

        override fun computeOnlyClassStatus(klass: FirClass<*>) {
            val existedStatus = statusMap.getValue(klass)
            if (existedStatus < StatusComputationStatus.ComputedOnlyClassStatus) {
                statusMap[klass] = StatusComputationStatus.ComputedOnlyClassStatus
            }
        }
    }

    class ForLocalClassResolution(private val localClasses: Set<FirClassLikeDeclaration<*>>) : StatusComputationSession() {
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

        override fun computeOnlyClassStatus(klass: FirClass<*>) {
            delegate.computeOnlyClassStatus(klass)
        }
    }
}

abstract class AbstractFirStatusResolveTransformer(
    final override val session: FirSession,
    val scopeSession: ScopeSession,
    protected val statusComputationSession: StatusComputationSession,
    protected val designationMapForLocalClasses: Map<FirClassLikeDeclaration<*>, FirClassLikeDeclaration<*>?>,
    private val scopeForLocalClass: FirScope?
) : FirAbstractTreeTransformer<FirResolvedDeclarationStatus?>(phase = FirResolvePhase.STATUS) {
    protected val classes = mutableListOf<FirClass<*>>()
    protected val statusResolver = FirStatusResolver(session, scopeSession)

    protected val containingClass: FirClass<*>? get() = classes.lastOrNull()

    protected abstract fun FirDeclaration.needResolveMembers(): Boolean
    protected abstract fun FirDeclaration.needResolveNestedClassifiers(): Boolean

    override fun transformFile(file: FirFile, data: FirResolvedDeclarationStatus?): FirFile {
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
        return file
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: FirResolvedDeclarationStatus?
    ): FirDeclarationStatus {
        return (data ?: declarationStatus)
    }

    protected inline fun storeClass(
        klass: FirClass<*>,
        computeResult: () -> FirDeclaration
    ): FirDeclaration {
        classes += klass
        val result = computeResult()
        classes.removeAt(classes.lastIndex)
        return result
    }

    override fun transformDeclaration(
        declaration: FirDeclaration,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration {
        declaration.replaceResolvePhase(transformerPhase)
        return when (declaration) {
            is FirCallableDeclaration<*> -> {
                when (declaration) {
                    is FirFunction<*> -> {
                        for (valueParameter in declaration.valueParameters) {
                            transformValueParameter(valueParameter, data)
                        }
                    }
                }
                declaration
            }
            else -> {
                transformElement(declaration, data)
            }
        }
    }

    override fun transformTypeAlias(
        typeAlias: FirTypeAlias,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration {
        typeAlias.typeParameters.forEach { transformDeclaration(it, data) }
        typeAlias.transformStatus(this, statusResolver.resolveStatus(typeAlias, containingClass, isLocal = false))
        return transformDeclaration(typeAlias, data)
    }

    abstract override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: FirResolvedDeclarationStatus?
    ): FirStatement

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        @Suppress("UNCHECKED_CAST")
        return transformClass(anonymousObject, data)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <F : FirClass<F>> transformClass(
        klass: FirClass<F>,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
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
            klass
        } as FirStatement
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
            forceResolveStatusOfCorrespondingClass(superTypeRef)
        }
    }

    private fun forceResolveStatusOfCorrespondingClass(typeRef: FirTypeRef) {
        when (val superClass = typeRef.coneType.toSymbol(session)?.fir) {
            is FirRegularClass -> forceResolveStatusesOfClass(superClass)
            is FirTypeAlias -> forceResolveStatusOfCorrespondingClass(superClass.expandedTypeRef)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun forceResolveStatusesOfClass(regularClass: FirRegularClass) {
        if (regularClass.origin == FirDeclarationOrigin.Java) {
            /*
             * If regular class has no corresponding file then it is platform class,
             *   so we need to resolve supertypes of this class because they could
             *   come from kotlin sources
             */
            forceResolveStatusesOfSupertypes(regularClass)
            return
        }
        if (regularClass.resolvePhase > FirResolvePhase.STATUS) return
        val firProvider = session.firProvider
        val statusComputationStatus = statusComputationSession[regularClass]
        if (!statusComputationStatus.requiresComputation) return

        if (regularClass.status is FirResolvedDeclarationStatus && statusComputationStatus == StatusComputationSession.StatusComputationStatus.Computed) {
            statusComputationSession.endComputing(regularClass)
            return
        }
        val symbol = regularClass.symbol
        val designation = if (regularClass.isLocal) buildList {
            var klass: FirClassLikeDeclaration<*> = regularClass
            while (true) {
                this.add(klass)
                klass = designationMapForLocalClasses[klass]?.takeIf { it !is FirAnonymousObject } ?: break
            }
            reverse()
        } else buildList<FirDeclaration> {
            val outerClasses = generateSequence(symbol.classId) { classId ->
                classId.outerClassId
            }.mapTo(mutableListOf()) { firProvider.getFirClassifierByFqName(it) }
            val file = firProvider.getFirClassifierContainerFileIfAny(regularClass.symbol)
            requireNotNull(file) { "Containing file was not found for\n${regularClass.render()}" }
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
        designation.first().transformSingle(transformer, null)
        statusComputationSession.endComputing(regularClass)
    }

    private fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        containingProperty: FirProperty,
    ) {
        propertyAccessor.transformStatus(
            this,
            statusResolver.resolveStatus(propertyAccessor, containingClass, containingProperty, isLocal = false)
        )

        propertyAccessor.replaceResolvePhase(transformerPhase)
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration {
        constructor.transformStatus(this, statusResolver.resolveStatus(constructor, containingClass, isLocal = false))
        return transformDeclaration(constructor, data)
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration {
        simpleFunction.replaceResolvePhase(transformerPhase)
        simpleFunction.transformStatus(this, statusResolver.resolveStatus(simpleFunction, containingClass, isLocal = false))
        return transformDeclaration(simpleFunction, data)
    }

    override fun transformProperty(
        property: FirProperty,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration {
        property.replaceResolvePhase(transformerPhase)
        property.transformStatus(this, statusResolver.resolveStatus(property, containingClass, isLocal = false))

        property.getter?.let { transformPropertyAccessor(it, property) }
        property.setter?.let { transformPropertyAccessor(it, property) }

        return property
    }

    override fun transformField(
        field: FirField,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration {
        field.transformStatus(this, statusResolver.resolveStatus(field, containingClass, isLocal = false))
        return transformDeclaration(field, data)
    }

    override fun transformEnumEntry(
        enumEntry: FirEnumEntry,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration {
        enumEntry.transformStatus(this, statusResolver.resolveStatus(enumEntry, containingClass, isLocal = false))
        return transformDeclaration(enumEntry, data)
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        @Suppress("UNCHECKED_CAST")
        return transformDeclaration(valueParameter, data) as FirStatement
    }

    override fun transformTypeParameter(
        typeParameter: FirTypeParameter,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration {
        return transformDeclaration(typeParameter, data)
    }

    override fun transformBlock(block: FirBlock, data: FirResolvedDeclarationStatus?): FirStatement {
        return block
    }
}

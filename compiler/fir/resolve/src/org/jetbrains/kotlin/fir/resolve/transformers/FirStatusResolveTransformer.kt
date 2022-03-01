/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.jetbrains.kotlin.fir.scopes.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
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

fun <F : FirClassLikeDeclaration> F.runStatusResolveForLocalClass(
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
    abstract fun tryCalculateResolvedStatus(declaration: FirCallableDeclaration): FirResolvedDeclarationStatus

    object Default : ResolvedStatusCalculator() {
        override fun tryCalculateResolvedStatus(declaration: FirCallableDeclaration): FirResolvedDeclarationStatus {
            val status = declaration.status
            require(status is FirResolvedDeclarationStatus) {
                "Status of ${declaration.render()} is unresolved"
            }
            return status
        }
    }
}

open class FirStatusResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    statusComputationSession: StatusComputationSession,
    designationMapForLocalClasses: Map<FirClassLikeDeclaration, FirClassLikeDeclaration?> = mapOf(),
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
    private val targetClass: FirClassLikeDeclaration,
    statusComputationSession: StatusComputationSession,
    designationMapForLocalClasses: Map<FirClassLikeDeclaration, FirClassLikeDeclaration?>,
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
        regularClass.symbol.ensureResolved(FirResolvePhase.TYPES)
        val classLocated = this.classLocated
        /*
         * In designated status resolve we should resolve status only of target class and it's members
         */
        if (classLocated) {
            assert(regularClass == targetClass)
            val computationStatus = statusComputationSession.startComputing(regularClass)
            forceResolveStatusesOfSupertypes(regularClass)
            if (computationStatus != StatusComputationSession.StatusComputationStatus.Computed) {
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
    abstract operator fun get(klass: FirClass): StatusComputationStatus

    abstract fun startComputing(klass: FirClass): StatusComputationStatus
    abstract fun endComputing(klass: FirClass)
    abstract fun computeOnlyClassStatus(klass: FirClass)

    enum class StatusComputationStatus(val requiresComputation: Boolean) {
        NotComputed(true),
        Computing(false),
        ComputedOnlyClassStatus(true),
        Computed(false)
    }

    class Regular : StatusComputationSession() {
        private val statusMap = mutableMapOf<FirClass, StatusComputationStatus>()
            .withDefault { StatusComputationStatus.NotComputed }

        override fun get(klass: FirClass): StatusComputationStatus = statusMap.getValue(klass)

        override fun startComputing(klass: FirClass): StatusComputationStatus {
            return statusMap.getOrPut(klass) { StatusComputationStatus.Computing }
        }

        override fun endComputing(klass: FirClass) {
            statusMap[klass] = StatusComputationStatus.Computed
        }

        override fun computeOnlyClassStatus(klass: FirClass) {
            val existedStatus = statusMap.getValue(klass)
            if (existedStatus < StatusComputationStatus.ComputedOnlyClassStatus) {
                statusMap[klass] = StatusComputationStatus.ComputedOnlyClassStatus
            }
        }
    }

    class ForLocalClassResolution(private val localClasses: Set<FirClassLikeDeclaration>) : StatusComputationSession() {
        private val delegate = Regular()

        override fun get(klass: FirClass): StatusComputationStatus {
            if (klass !in localClasses) return StatusComputationStatus.Computed
            return delegate[klass]
        }

        override fun startComputing(klass: FirClass): StatusComputationStatus {
            return delegate.startComputing(klass)
        }

        override fun endComputing(klass: FirClass) {
            delegate.endComputing(klass)
        }

        override fun computeOnlyClassStatus(klass: FirClass) {
            delegate.computeOnlyClassStatus(klass)
        }
    }
}

abstract class AbstractFirStatusResolveTransformer(
    final override val session: FirSession,
    val scopeSession: ScopeSession,
    protected val statusComputationSession: StatusComputationSession,
    protected val designationMapForLocalClasses: Map<FirClassLikeDeclaration, FirClassLikeDeclaration?>,
    private val scopeForLocalClass: FirScope?
) : FirAbstractTreeTransformer<FirResolvedDeclarationStatus?>(phase = FirResolvePhase.STATUS) {
    protected val classes = mutableListOf<FirClass>()
    protected val statusResolver = FirStatusResolver(session, scopeSession)

    protected val containingClass: FirClass? get() = classes.lastOrNull()

    protected abstract fun FirDeclaration.needResolveMembers(): Boolean
    protected abstract fun FirDeclaration.needResolveNestedClassifiers(): Boolean

    override fun transformFile(file: FirFile, data: FirResolvedDeclarationStatus?): FirFile {
        transformDeclarationContent(file, data)
        return file
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: FirResolvedDeclarationStatus?
    ): FirDeclarationStatus {
        return (data ?: declarationStatus)
    }

    protected inline fun storeClass(
        klass: FirClass,
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
        return when (declaration) {
            is FirCallableDeclaration -> {
                if (declaration is FirFunction) {
                    for (valueParameter in declaration.valueParameters) {
                        transformValueParameter(valueParameter, data)
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
    ): FirStatement {
        typeAlias.typeParameters.forEach { transformDeclaration(it, data) }
        typeAlias.transformStatus(this, statusResolver.resolveStatus(typeAlias, containingClass, isLocal = false))
        return transformDeclaration(typeAlias, data) as FirTypeAlias
    }

    abstract override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: FirResolvedDeclarationStatus?
    ): FirStatement

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        anonymousObject.transformStatus(
            this,
            FirResolvedDeclarationStatusImpl(
                anonymousObject.status.visibility,
                anonymousObject.status.modality ?: Modality.FINAL,
                EffectiveVisibility.Local
            )
        )
        @Suppress("UNCHECKED_CAST")
        return transformClass(anonymousObject, data)
    }

    open fun transformDeclarationContent(
        declaration: FirDeclaration,
        data: FirResolvedDeclarationStatus?
    ): FirDeclaration {

        val declarations = when (declaration) {
            is FirRegularClass -> declaration.declarations
            is FirAnonymousObject -> declaration.declarations
            is FirFile -> declaration.declarations
            else -> error("Not supported declaration ${declaration::class.simpleName}")
        }

        if (declaration.needResolveMembers()) {
            declarations.forEach {
                if (it !is FirClassLikeDeclaration) {
                    it.transformSingle(this, data)
                }
            }
        }
        if (declaration.needResolveNestedClassifiers()) {
            declarations.forEach {
                if (it is FirClassLikeDeclaration) {
                    it.transformSingle(this, data)
                }
            }
        }
        return declaration
    }

    @Suppress("UNCHECKED_CAST")
    override fun transformClass(
        klass: FirClass,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        return storeClass(klass) {
            klass.typeParameters.forEach { it.transformSingle(this, data) }
            transformDeclarationContent(klass, data)
        } as FirStatement
    }

    protected fun forceResolveStatusesOfSupertypes(regularClass: FirRegularClass) {
        for (superTypeRef in regularClass.superTypeRefs) {
            forceResolveStatusOfCorrespondingClass(superTypeRef)
        }
    }

    private fun forceResolveStatusOfCorrespondingClass(typeRef: FirTypeRef) {
        val superClassSymbol = typeRef.coneType.toSymbol(session)
        superClassSymbol?.ensureResolved(FirResolvePhase.SUPER_TYPES)
        when (superClassSymbol) {
            is FirRegularClassSymbol -> forceResolveStatusesOfClass(superClassSymbol.fir)
            is FirTypeAliasSymbol -> forceResolveStatusOfCorrespondingClass(superClassSymbol.fir.expandedTypeRef)
            is FirTypeParameterSymbol, is FirAnonymousObjectSymbol, null -> {}
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun forceResolveStatusesOfClass(regularClass: FirRegularClass) {
        if (regularClass.origin == FirDeclarationOrigin.Java || regularClass.origin == FirDeclarationOrigin.Precompiled) {
            /*
             * If regular class has no corresponding file then it is platform class,
             *   so we need to resolve supertypes of this class because they could
             *   come from kotlin sources
             */
            forceResolveStatusesOfSupertypes(regularClass)
            return
        }
        if (regularClass.origin != FirDeclarationOrigin.Source) return
        val statusComputationStatus = statusComputationSession[regularClass]
        if (!statusComputationStatus.requiresComputation) return

        if (regularClass.status is FirResolvedDeclarationStatus && statusComputationStatus == StatusComputationSession.StatusComputationStatus.Computed) {
            statusComputationSession.endComputing(regularClass)
            return
        }
        val symbol = regularClass.symbol
        val designation = if (regularClass.isLocal) buildList {
            var klass: FirClassLikeDeclaration = regularClass
            while (true) {
                this.add(klass)
                klass = designationMapForLocalClasses[klass]?.takeIf { it !is FirAnonymousObject } ?: break
            }
            reverse()
        } else buildList<FirDeclaration> {
            val firProvider = regularClass.moduleData.session.firProvider
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
        overriddenStatuses: List<FirResolvedDeclarationStatus> = emptyList(),
    ) {
        propertyAccessor.transformStatus(
            this,
            statusResolver.resolveStatus(
                propertyAccessor,
                containingClass,
                containingProperty,
                isLocal = false,
                overriddenStatuses,
            )
        )

        propertyAccessor.transformValueParameters(this, null)
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        constructor.transformStatus(this, statusResolver.resolveStatus(constructor, containingClass, isLocal = false))
        calculateDeprecations(constructor)
        return transformDeclaration(constructor, data) as FirStatement
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        val resolvedStatus = statusResolver.resolveStatus(simpleFunction, containingClass, isLocal = false)
        simpleFunction.transformStatus(this, resolvedStatus)
        calculateDeprecations(simpleFunction)
        return transformDeclaration(simpleFunction, data) as FirStatement
    }

    override fun transformProperty(
        property: FirProperty,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        val overridden = statusResolver.getOverriddenProperties(property, containingClass)

        val overriddenProperties = overridden.map {
            it.ensureResolved(FirResolvePhase.STATUS)
            it.status as FirResolvedDeclarationStatus
        }

        val overriddenSetters = overridden.mapNotNull {
            it.setter?.ensureResolved(FirResolvePhase.STATUS)
            it.setter?.status as? FirResolvedDeclarationStatus
        }

        property.transformStatus(
            this,
            statusResolver.resolveStatus(property, containingClass, false, overriddenProperties)
        )

        property.getter?.let { transformPropertyAccessor(it, property) }
        property.setter?.let { transformPropertyAccessor(it, property, overriddenSetters) }

        property.backingField?.let {
            it.transformStatus(
                this,
                statusResolver.resolveStatus(it, containingClass, property, isLocal = false)
            )
        }

        calculateDeprecations(property)
        return property
    }

    override fun transformField(
        field: FirField,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        field.transformStatus(this, statusResolver.resolveStatus(field, containingClass, isLocal = false))
        calculateDeprecations(field)
        return transformDeclaration(field, data) as FirField
    }

    override fun transformEnumEntry(
        enumEntry: FirEnumEntry,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        enumEntry.transformStatus(this, statusResolver.resolveStatus(enumEntry, containingClass, isLocal = false))
        calculateDeprecations(enumEntry)
        return transformDeclaration(enumEntry, data) as FirEnumEntry
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: FirResolvedDeclarationStatus?
    ): FirStatement {
        calculateDeprecations(valueParameter)
        @Suppress("UNCHECKED_CAST")
        return transformDeclaration(valueParameter, data) as FirStatement
    }

    override fun transformTypeParameter(
        typeParameter: FirTypeParameter,
        data: FirResolvedDeclarationStatus?
    ): FirTypeParameterRef {
        return transformDeclaration(typeParameter, data) as FirTypeParameter
    }

    override fun transformBlock(block: FirBlock, data: FirResolvedDeclarationStatus?): FirStatement {
        return block
    }

    protected fun calculateDeprecations(simpleFunction: FirCallableDeclaration) {
        if (simpleFunction.deprecation == null) {
            simpleFunction.replaceDeprecation(simpleFunction.getDeprecationInfos(session.languageVersionSettings.apiVersion))
        }
    }
}

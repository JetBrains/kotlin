/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

public sealed class DeclarationBuildingContext<T : FirDeclaration>(
    protected val session: FirSession,
    protected val key: GeneratedDeclarationKey,
    protected val owner: FirClassSymbol<*>?
) {
    /**
     * Allows to set visibility of the declaration
     */
    public var visibility: Visibility = Visibilities.Public

    /**
     * Allows to set modality of the declaration
     */
    public var modality: Modality = Modality.FINAL

    /**
     * Allows to configure flags in status of declaration
     * For full list of possible flags refer to [FirDeclarationStatus] class
     * Note that not all flags are meaningful for each declaration
     *   E.g. there is no point to mark function as inner
     */
    public fun status(statusConfig: FirResolvedDeclarationStatusImpl.() -> Unit) {
        statusConfigs += statusConfig
    }

    /**
     * Adds type parameter with specified [name] and [variance] to declaration
     *
     * Upper bounds of type parameters can be configured in [config] lambda
     *
     * If no bounds passed then `kotlin.Any?` bound will be added automatically
     */
    public open fun typeParameter(
        name: Name,
        variance: Variance = Variance.INVARIANT,
        isReified: Boolean = false,
        key: GeneratedDeclarationKey = this@DeclarationBuildingContext.key,
        config: TypeParameterBuildingContext.() -> Unit = {}
    ) {
        typeParameters += TypeParameterData(name, variance, isReified, TypeParameterBuildingContext().apply(config).boundProviders, key)
    }

    public class TypeParameterBuildingContext {
        /**
         * Declares [type] as upper bound of type parameter
         */
        public fun bound(type: ConeKotlinType) {
            bound { type }
        }

        /**
         * Type produced by [typeProvider] will be an upper bound of the type parameter
         *
         * Use this method when bounds of your type parameters depend on each other
         *   For example, in this case:
         *     ```
         *     interface Out<out X>
         *
         *     fun <T, R> foo() where T : R, R : Out<T> {}
         *     ```
         */
        public fun bound(typeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType) {
            boundProviders += typeProvider
        }

        internal val boundProviders: MutableList<(List<FirTypeParameterRef>) -> ConeKotlinType> = mutableListOf()
    }


    private val contextReceiverTypeProviders: MutableList<(List<FirTypeParameterRef>) -> ConeKotlinType> = mutableListOf()

    /**
     * Adds context receiver with [type] type to declaration
     */
    public open fun contextReceiver(type: ConeKotlinType) {
        contextReceiver { type }
    }

    /**
     * Adds context receiver with type provided by [typeProvider] to declaration
     * Use this overload when context receiver type uses type parameters of constructed declaration
     */
    public open fun contextReceiver(typeProvider: (List<FirTypeParameterRef>) -> ConeKotlinType) {
        contextReceiverTypeProviders += typeProvider
    }

    protected fun produceContextReceiversTo(
        destination: MutableList<FirValueParameter>,
        typeParameters: List<FirTypeParameterRef>,
        origin: FirDeclarationOrigin,
        containingDeclarationSymbol: FirCallableSymbol<*>,
    ) {
        contextReceiverTypeProviders.mapTo(destination) {
            buildValueParameter {
                this.moduleData = session.moduleData
                this.origin = origin
                this.name = SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
                this.symbol = FirValueParameterSymbol(name)
                this.returnTypeRef = it.invoke(typeParameters).toFirResolvedTypeRef()
                this.containingDeclarationSymbol = containingDeclarationSymbol
                this.valueParameterKind = FirValueParameterKind.ContextParameter
            }
        }
    }

    protected data class TypeParameterData(
        val name: Name,
        val variance: Variance,
        val isReified: Boolean,
        val boundProviders: List<(List<FirTypeParameterRef>) -> ConeKotlinType>,
        val key: GeneratedDeclarationKey
    )

    protected val typeParameters: MutableList<TypeParameterData> = mutableListOf()

    private val statusConfigs: MutableList<FirResolvedDeclarationStatusImpl.() -> Unit> = mutableListOf()

    /**
     * Sets the source of the generated declaration.
     * If this property wasn't initialized, then the fake source element based on
     * the owner source will be created.
     */
    public var source: KtSourceElement?
        get() = _source as? KtSourceElement
        set(value) {
            _source = value
        }

    private var _source: Any? = DEFAULT_SOURCE_ELEMENT_STUB

    protected fun getSourceForFirDeclaration(): KtSourceElement? {
        if (_source === DEFAULT_SOURCE_ELEMENT_STUB) {
            return owner?.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated)
        }
        return _source as KtSourceElement?
    }

    public abstract fun build(): T

    protected fun generateStatus(): FirResolvedDeclarationStatusImpl {
        return FirResolvedDeclarationStatusImpl(
            visibility,
            modality,
            visibility.toEffectiveVisibility(owner, forClass = true)
        ).also {
            for (statusConfig in statusConfigs) {
                it.apply(statusConfig)
            }
        }
    }

    protected fun generateTypeParameter(
        typeParameter: TypeParameterData,
        containingDeclarationSymbol: FirBasedSymbol<*>
    ): FirTypeParameter {
        return buildTypeParameter {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = typeParameter.key.origin
            name = typeParameter.name
            symbol = FirTypeParameterSymbol()
            this.containingDeclarationSymbol = containingDeclarationSymbol
            variance = typeParameter.variance
            isReified = typeParameter.isReified
        }
    }

    protected fun initTypeParameterBounds(allParameters: List<FirTypeParameterRef>, ownTypeParameters: List<FirTypeParameter>) {
        for ((typeParameter, data) in ownTypeParameters.zip(typeParameters)) {
            val coneBounds = data.boundProviders.map { it.invoke(allParameters) }
            val bounds = if (coneBounds.isEmpty()) {
                listOf(session.builtinTypes.nullableAnyType)
            } else {
                coneBounds.map { it.toFirResolvedTypeRef() }
            }
            typeParameter.replaceBounds(bounds)
        }
    }

    private companion object {
        val DEFAULT_SOURCE_ELEMENT_STUB: Any = Any()
    }
}

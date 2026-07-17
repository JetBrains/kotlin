/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.generators.Fir2IrCallableDeclarationsGenerator
import org.jetbrains.kotlin.fir.backend.lazyMappedFunctionListVar
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionExtension
import org.jetbrains.kotlin.fir.initialSignatureAttr
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class Fir2IrLazySimpleFunction(
    c: Fir2IrComponents,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val fir: FirNamedFunction,
    private val firParent: FirRegularClass?,
    symbol: IrSimpleFunctionSymbol,
    parent: IrDeclarationParent,
    isFakeOverride: Boolean
) : AbstractFir2IrLazyFunction<FirNamedFunction>(c, startOffset, endOffset, origin, symbol, parent, isFakeOverride) {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
    }

    override var annotations: List<IrAnnotation> by createLazyAnnotations()

    override var name: Name
        get() = fir.name
        set(_) = mutationNotSupported()

    override var returnType: IrType by lazyVar(lock) {
        fir.symbol.resolvedReturnTypeRef.toIrType()
    }

    override val parameterStorage: MutableList<IrValueParameter> by lazy {
        declarationStorage.enterScope(this.symbol)

        buildList {
            val containingClass = parent as? IrClass
            if (containingClass != null && shouldHaveDispatchReceiver(containingClass)) {
                val thisType = context(c) {
                    Fir2IrCallableDeclarationsGenerator.computeDispatchReceiverType(
                        this@Fir2IrLazySimpleFunction,
                        fir,
                        containingClass,
                    )
                }
                add(
                    createThisReceiverParameter(
                        thisType ?: error("No dispatch receiver receiver for function"),
                        kind = IrParameterKind.DispatchReceiver,
                    )
                )
            }

            callablesGenerator.addContextParametersTo(
                fir.contextParameters,
                this@Fir2IrLazySimpleFunction,
                this@buildList
            )

            fir.receiverParameter?.takeUnless { fir.isCompanionExtension }?.let {
                add(
                    createThisReceiverParameter(it.typeRef.toIrType(typeConverter), IrParameterKind.ExtensionReceiver)
                )
            }

            fir.valueParameters.mapTo(this) { valueParameter ->
                callablesGenerator.createIrParameter(
                    valueParameter, skipDefaultParameter = isFakeOverride
                ).apply {
                    this.parent = this@Fir2IrLazySimpleFunction
                }
            }
        }.apply {
            declarationStorage.leaveScope(this@Fir2IrLazySimpleFunction.symbol)
        }.toMutableList()
    }

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> by symbolsMappingForLazyClasses.lazyMappedFunctionListVar(lock) lazy@{
        if (firParent == null || parent !is Fir2IrLazyClass) return@lazy emptyList()

        val baseFunctionWithDispatchReceiverTag =
            lazyFakeOverrideGenerator.computeFakeOverrideKeys(firParent, fir.symbol)
        baseFunctionWithDispatchReceiverTag.map { [symbol, dispatchReceiverLookupTag] ->
            declarationStorage.getIrFunctionSymbol(symbol, dispatchReceiverLookupTag) as IrSimpleFunctionSymbol
        }
    }

    override val initialSignatureFunction: IrFunction? by lazy {
        val originalFunction = fir.initialSignatureAttr ?: return@lazy null
        val lookupTag = firParent?.symbol?.toLookupTag()

        // `initialSignatureFunction` is not called during fir2ir conversion
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        declarationStorage.getIrFunctionSymbol(originalFunction, lookupTag).owner.also {
            check(it !== this) { "Initial function can not be the same as remapped function" }
        }
    }

    override val containerSource: DeserializedContainerSource?
        get() = fir.containerSource

    override var companionExtensionClass: IrClassSymbol?
        get() = fir.receiverParameter?.takeIf { fir.isCompanionExtension }?.typeRef?.toIrType()?.classOrFail
        set(_) = mutationNotSupported()
}

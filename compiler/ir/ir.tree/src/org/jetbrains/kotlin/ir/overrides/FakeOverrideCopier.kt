/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.linkage.partial.IrUnimplementedOverridesStrategy
import org.jetbrains.kotlin.ir.util.*

class FakeOverrideCopier(
    private val symbolRemapper: SymbolRemapper,
    private val typeRemapper: TypeRemapper,
    private val symbolRenamer: SymbolRenamer,
    private val makeExternal: Boolean,
    private val parent: IrClass,
    private val unimplementedOverridesStrategy: IrUnimplementedOverridesStrategy
) : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper, symbolRenamer) {

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        val customization = unimplementedOverridesStrategy.computeCustomization(declaration, parent)

        return declaration.factory.createFunctionWithLateBinding(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = customization.origin ?: IrDeclarationOrigin.FAKE_OVERRIDE,
            name = symbolRenamer.getFunctionName(declaration.symbol),
            visibility = declaration.visibility,
            isInline = declaration.isInline,
            isExpect = declaration.isExpect,
            returnType = declaration.returnType,
            modality = customization.modality ?: declaration.modality,
            isTailrec = declaration.isTailrec,
            isSuspend = declaration.isSuspend,
            isOperator = declaration.isOperator,
            isInfix = declaration.isInfix,
            isExternal = makeExternal,
        ).apply {
            transformAnnotations(declaration)
            copyTypeParametersFrom(declaration)
            typeRemapper.withinScope(this) {
                // This is the more correct way to produce dispatch receiver for a fake override,
                // but some lowerings still expect the below behavior as produced by the current psi2ir.
                /*
                    val superDispatchReceiver = declaration.dispatchReceiverParameter!!
                    val dispatchReceiverSymbol = IrValueParameterSymbolImpl(WrappedReceiverParameterDescriptor())
                    val dispatchReceiverType = destinationClass.defaultType
                    dispatchReceiverParameter = IrValueParameterImpl(
                        superDispatchReceiver.startOffset,
                        superDispatchReceiver.endOffset,
                        superDispatchReceiver.origin,
                        dispatchReceiverSymbol,
                        superDispatchReceiver.name,
                        superDispatchReceiver.index,
                        dispatchReceiverType,
                        null,
                        superDispatchReceiver.isCrossinline,
                        superDispatchReceiver.isNoinline
                    )
                    */
                // Should fake override's receiver be the current class is an open question.
                dispatchReceiverParameter = declaration.dispatchReceiverParameter?.transform()
                extensionReceiverParameter = declaration.extensionReceiverParameter?.transform()
                returnType = typeRemapper.remapType(declaration.returnType)
                valueParameters = declaration.valueParameters.transform()
            }
        }
    }

    override fun visitProperty(declaration: IrProperty): IrProperty {
        val customization = unimplementedOverridesStrategy.computeCustomization(declaration, parent)

        return declaration.factory.createPropertyWithLateBinding(
            declaration.startOffset, declaration.endOffset,
            customization.origin ?: IrDeclarationOrigin.FAKE_OVERRIDE,
            declaration.name,
            declaration.visibility,
            customization.modality ?: declaration.modality,
            isVar = declaration.isVar,
            isConst = declaration.isConst,
            isLateinit = declaration.isLateinit,
            isDelegated = declaration.isDelegated,
            isExpect = declaration.isExpect,
            isExternal = makeExternal
        ).apply {
            transformAnnotations(declaration)
            this.getter = declaration.getter?.transform()
            this.setter = declaration.setter?.transform()
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter): IrValueParameter =
        declaration.factory.createValueParameter(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = symbolRenamer.getValueParameterName(declaration.symbol),
            type = declaration.type.remapType(),
            isAssignable = declaration.isAssignable,
            symbol = symbolRemapper.getDeclaredValueParameter(declaration.symbol),
            index = declaration.index,
            varargElementType = declaration.varargElementType?.remapType(),
            isCrossinline = declaration.isCrossinline,
            isNoinline = declaration.isNoinline,
            isHidden = declaration.isHidden,
        ).apply {
            transformAnnotations(declaration)
            // Don't set the default value for fake overrides.
        }
}

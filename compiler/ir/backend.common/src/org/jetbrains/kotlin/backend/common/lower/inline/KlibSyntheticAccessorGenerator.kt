/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

// TODO: use some class to bear information about the inline function where the accessor is needed
typealias InlineFunctionInfo = Nothing?

class KlibSyntheticAccessorGenerator(
    context: CommonBackendContext
) : SyntheticAccessorGenerator<CommonBackendContext, InlineFunctionInfo>(context) {

    private data class OuterThisAccessorKey(val innerClass: IrClass)

    companion object {
        const val TOP_LEVEL_FUNCTION_SUFFIX_MARKER = "t"

        private var IrValueParameter.outerThisSyntheticAccessors: MutableMap<OuterThisAccessorKey, IrSimpleFunction>? by irAttribute(
            followAttributeOwner = false
        )
    }

    override fun IrConstructor.makeConstructorAccessor(originForConstructorAccessor: IrDeclarationOrigin): IrFunction {
        val source = this

        return factory.buildFun {
            startOffset = parent.startOffset
            endOffset = parent.startOffset
            origin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = source.accessorNameForStaticConstructor()
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
        }.also { accessor ->
            accessor.parent = parent

            val klass = source.classIfConstructor
            accessor.copyTypeParametersFrom(klass, IrDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.copyValueParametersToStatic(source, IrDeclarationOrigin.SYNTHETIC_ACCESSOR)
            accessor.returnType = source.returnType.remapTypeParameters(klass, accessor)

            accessor.body = context.irFactory.createExpressionBody(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                createConstructorCall(accessor, source.symbol)
            )
        }
    }

    private fun createConstructorCall(accessor: IrFunction, targetSymbol: IrConstructorSymbol) =
        IrConstructorCallImpl.fromSymbolOwner(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            accessor.returnType,
            targetSymbol, targetSymbol.owner.parentAsClass.typeParameters.size + targetSymbol.owner.typeParameters.size
        ).also {
            copyAllParamsToArgs(it, accessor)
        }

    override fun accessorModality(parent: IrDeclarationParent) = Modality.FINAL
    override fun IrDeclarationWithVisibility.accessorParent(parent: IrDeclarationParent, scopeInfo: InlineFunctionInfo) = parent

    override fun AccessorNameBuilder.buildFunctionName(
        function: IrSimpleFunction,
        superQualifier: IrClassSymbol?,
        scopeInfo: InlineFunctionInfo,
    ) {
        contribute(function.name.asString())

        val parent = function.parent
        if (parent is IrPackageFragment) {
            // This is a top-level function. Include the sanitized .kt file name to avoid potential clashes.
            check(parent is IrFile) {
                "Unexpected type of package fragment for top-level function ${function.render()}: ${parent::class.java}, ${parent.render()}"
            }

            contribute(TOP_LEVEL_FUNCTION_SUFFIX_MARKER + parent.packagePartClassName)
        }
    }

    override fun AccessorNameBuilder.buildFieldGetterName(field: IrField, superQualifierSymbol: IrClassSymbol?) {
        contribute("<get-${field.name}>")
        contribute(PROPERTY_MARKER)
    }

    override fun AccessorNameBuilder.buildFieldSetterName(field: IrField, superQualifierSymbol: IrClassSymbol?) {
        contribute("<set-${field.name}>")
        contribute(PROPERTY_MARKER)
    }

    private fun IrConstructor.accessorNameForStaticConstructor(): Name =
        AccessorNameBuilder().apply { contribute(this@accessorNameForStaticConstructor.name.asString()) }.build()

    /**
     * This is a special kind of _private_ non-static accessor specifically for accessing "outer this"
     * implicit value parameter from within the scope of the class instance. This accessor is not intended
     * to be a part of ABI. The accessor is created by [OuterThisInInlineFunctionsSpecialAccessorLowering].
     *
     * Note: A new non-private static accessor may be generated later in [SyntheticAccessorLowering] as
     * a wrapper for _this_ accessor allowing calling it inside inline functions (and as a result getting "outer this").
     * The new static accessor may become a part of public ABI if it has _public_ visibility.
     */
    fun getSyntheticOuterThisParameterAccessor(
        expression: IrGetValue,
        outerThisValueParameter: IrValueParameter,
        innerClass: IrClass
    ): IrSimpleFunction {
        val functionMap = outerThisValueParameter.outerThisSyntheticAccessors
            ?: hashMapOf<OuterThisAccessorKey, IrSimpleFunction>().also { outerThisValueParameter.outerThisSyntheticAccessors = it }

        return functionMap.getOrPut(OuterThisAccessorKey(innerClass)) {
            makeSyntheticOuterThisParameterAccessor(expression, innerClass, outerThisValueParameter.parentAsClass)
        }
    }

    private fun makeSyntheticOuterThisParameterAccessor(
        expression: IrGetValue,
        innerClass: IrClass,
        outerClass: IrClass
    ): IrSimpleFunction {
        val levelDifference = innerClass.parents.filterIsInstance<IrClass>().takeWhile { it != outerClass }.count()

        // "<outer-this-0>" for the closest outer class, "<outer-this-1>" for the next one, and so on.
        // Note: The static public accessor for call sites of this accessor in non-private inline functions would
        // get a derived name with the "access" prefix. Example: "access$<outer-this-1>".
        val accessorName = Name.identifier("<outer-this-$levelDifference>")
        val innerClassThisReceiver = innerClass.thisReceiver!!

        return innerClass.factory.buildFun {
            startOffset = innerClass.startOffset
            endOffset = innerClass.startOffset
            origin = IrDeclarationOrigin.SYNTHETIC_ACCESSOR
            name = accessorName
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            parent = innerClass
            dispatchReceiverParameter = innerClassThisReceiver.copyTo(
                this,
                IrDeclarationOrigin.SYNTHETIC_ACCESSOR,
                type = innerClassThisReceiver.type // This is the type of the inner class.
            )
            returnType = expression.type // This is the type of the outer class.
            body = context.irFactory.createExpressionBody(startOffset, startOffset, expression)
        }
    }
}

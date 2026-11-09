/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.isRestrictedSuspensionFunction
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrRichPropertyReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.runIf

open class UpgradeCallableReferences(
    val context: LoweringContext,
    val upgradeSamConversions: Boolean = true,
    val castDispatchReceiver: Boolean = true,
    val generateFakeAccessorsForReflectionProperty: Boolean = false,
) : FileLoweringPass {

    override fun lower(irFile: IrFile) { TODO() }

    fun lower(irFunction: IrFunction) { TODO() }

    private data class AdaptedBlock(
        val function: IrSimpleFunction,
        val reference: IrFunctionReference,
        val samConversionType: IrType?,
        val referenceType: IrType,
    )

    private data class CapturedValue(
        val name: Name,
        val type: IrType,
        val correspondingParameter: IrValueParameter?,
        val expression: IrExpression,
    )

    private inner class UpgradeTransformer : IrTransformer<IrDeclarationParent>() {
        private fun IrFunction.flattenParameters(isLambda: Boolean) { TODO() }

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: IrDeclarationParent): IrElement { TODO() }

        override fun visitElement(element: IrElement, data: IrDeclarationParent): IrElement { TODO() }

        // IrTransformer defines this to not calling visitElement, which leads to incorrect parent creation
        override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent): IrStatement { TODO() }

        override fun visitFile(declaration: IrFile, data: IrDeclarationParent): IrFile { TODO() }

        private fun IrType.arrayDepth(): Int { TODO() }

        private fun hasVarargConversion(wrapper: IrSimpleFunction, target: IrSimpleFunction): Boolean { TODO() }

        private val blockReferenceOrigins = setOf(
            IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE, IrStatementOrigin.FUNCTION_TYPE_EXPRESSION_CONVERSION,
            IrStatementOrigin.LAMBDA, IrStatementOrigin.INLINE_LAMBDA, IrStatementOrigin.FUN_INTERFACE_CONSTRUCTOR_REFERENCE, IrStatementOrigin.ANONYMOUS_FUNCTION,
        )

        private fun IrBlock.parseAdaptedBlock() : AdaptedBlock? { TODO() }

        override fun visitBlock(expression: IrBlock, data: IrDeclarationParent): IrExpression { TODO() }

        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrDeclarationParent): IrExpression { TODO() }

        private fun IrCallableReference<*>.getCapturedValues() = buildList {
            for ([parameter, argument] in getArgumentsWithIr()) {
                add(CapturedValue(parameter.name, argument.type, parameter, argument))
            }
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclarationParent): IrExpression { TODO() }

        override fun visitPropertyReference(expression: IrPropertyReference, data: IrDeclarationParent): IrExpression { TODO() }

        /**
         * KLIBs don't contain enough information for initializing the correct shape for [IrFunctionReference]s and [IrPropertyReference]s.
         *
         * For example, consider the following code:
         *
         * ```kotlin
         * class C {
         *     fun foo() {}
         * }
         *
         * fun bar() {}
         * ```
         *
         * Function references `C::foo` and `::bar` will both be serialized (and deserialized) as having the following shape:
         * ```
         * dispatch_receiver = null
         * extension_receiver = null
         * value_argument = []
         * ```
         *
         * However, `C::foo` has unbound dispatch receiver, while `::bar` doesn't have any dispatch receiver.
         * To be able to adopt the new value parameter API ([KT-71850](https://youtrack.jetbrains.com/issue/KT-71850)),
         * we have to be able to distinguish these two situations, because for `C::foo` the target function's [IrFunction.parameters]
         * will be [[dispatch receiver]], while for `::bar` the target function's [IrFunction.parameters] will be an empty list,
         * and [IrFunctionReference.arguments] must always match the target function's [IrFunction.parameters] list.
         *
         * The same applies to [IrPropertyReference].
         *
         * Because existing KLIBs already don't contain enough information for setting the correct shape, the following hack is used:
         * After linking we visit callable references and update their shape from the linked target function/property.
         *
         * See [KT-71849](https://youtrack.jetbrains.com/issue/KT-71849).
         */
        private fun fixCallableReferenceComingFromKlib(reference: IrCallableReference<*>) { TODO() }

        override fun visitLocalDelegatedPropertyReference(
            expression: IrLocalDelegatedPropertyReference,
            data: IrDeclarationParent
        ): IrExpression { TODO() }

        private fun IrCallableReference<*>.buildUnsupportedForLocalFunction(
            captured: List<CapturedValue>,
            parent: IrDeclarationParent,
            name: Name,
            isSuspend: Boolean,
            isPropertySetter: Boolean,
        ) = buildWrapperFunction(captured, parent, name, isSuspend, isPropertySetter) { _, _ ->
            +irCall(this@UpgradeCallableReferences.context.symbols.throwUnsupportedOperationException).apply {
                arguments[0] = irString("Not supported for local property reference.")
            }
        }.apply {
            returnType = context.irBuiltIns.nothingType
        }

        private fun IrCallableReference<*>.buildReflectionPropertyAccessorWithoutBody(
            captured: List<CapturedValue>,
            parent: IrDeclarationParent,
            name: Name,
            isSuspend: Boolean,
            isPropertySetter: Boolean,
        ) = buildWrapperFunction(captured, parent, name, isSuspend, isPropertySetter, body = null)

        private fun IrCallableReference<*>.buildWrapperFunction(
            captured: List<CapturedValue>,
            parent: IrDeclarationParent,
            name: Name,
            isSuspend: Boolean,
            isPropertySetter: Boolean,
            body: (IrBlockBodyBuilder.(List<IrValueParameter>, IrType) -> Unit)?,
        ): IrSimpleFunction { TODO() }

        private fun IrPropertyReference.wrapField(
            captured: List<CapturedValue>,
            parent: IrDeclarationParent,
            field: IrField,
            isPropertySetter: Boolean
        ): IrSimpleFunction { TODO() }

        private fun IrCallableReference<*>.wrapFunction(
            captured: List<CapturedValue>,
            parent: IrDeclarationParent,
            referencedFunction: IrFunction,
            isPropertySetter: Boolean = false
        ): IrSimpleFunction { TODO() }

        private fun orderParametersToForward(
            captured: List<CapturedValue>,
            referencedFunctionParameters: List<IrValueParameter>,
            wrapperFunctionParameters: List<IrValueParameter>,
        ): List<IrValueParameter> { TODO() }
    }
}

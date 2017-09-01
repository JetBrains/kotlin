/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.lower.DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.js.builtins.JsBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.endOffsetOrUndefined
import org.jetbrains.kotlin.ir.util.startOffsetOrUndefined
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOrInheritsParametersWithDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.hasOwnParametersWithDefaultValue
import org.jetbrains.kotlin.types.*

class DefaultArgumentLowering(
        private val jsBuiltIns: JsBuiltIns,
        private val builtIns: IrBuiltIns
) : DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction)
                lower(memberDeclaration)
            else
                null
        }
    }

    private fun lower(irFunction: IrFunction): List<IrDeclaration>? {
        if (!irFunction.descriptor.hasOrInheritsParametersWithDefaultValue()) return null

        val result = mutableListOf(irFunction)

        if (irFunction.descriptor.hasOwnParametersWithDefaultValue()) {
            if (irFunction.descriptor.isOverridable) {
                generateBodyFunction(irFunction).also { result += it }
            }
        }
        else {
            generateBodyFunction(irFunction).let { result += it }
        }

        val irBody = irFunction.body
        if (irBody != null) {
            val blockBody = when (irBody) {
                is IrBlockBody -> irBody
                is IrExpressionBody -> IrBlockBodyImpl(irBody.startOffset, irBody.endOffset, listOf(irBody.expression))
                else -> error("Unknown body type: ${irBody::class.java.name}")
            }
            blockBody.statements.addAll(0, generateDefaultArgumentInitializer(irFunction))
        }

        return result
    }

    private fun generateBodyFunction(irFunction: IrFunction): IrFunction {
        val newFunction = irFunction.descriptor.generateDefaultsFunction()
        val valueParameterMap = irFunction.valueParameters.map { it.symbol }.zip(newFunction.valueParameters.map { it.symbol }).toMap()
        newFunction.body = irFunction.body?.let { mapParameters(it) { valueParameterMap[it] } }

        irFunction.body = IrBlockBodyImpl(irFunction.startOffset, irFunction.endOffset).apply {
            val typeArguments = irFunction.descriptor.typeParameters.zip(newFunction.descriptor.typeParameters)
                    .map { (oldParam, newParam) -> newParam to oldParam.defaultType }
                    .toMap()

            val delegateCall = IrCallImpl(
                    newFunction.startOffset, irFunction.endOffset,
                    newFunction.symbol, newFunction.descriptor,
                    typeArguments = typeArguments)
            with (delegateCall) {
                dispatchReceiver = irFunction.dispatchReceiverParameter?.let {
                    IrGetValueImpl(it.startOffset, it.endOffset, it.symbol)
                }
                extensionReceiver = irFunction.extensionReceiverParameter?.let {
                    IrGetValueImpl(it.startOffset, it.endOffset, it.symbol)
                }

                for ((index, parameter) in irFunction.valueParameters.withIndex()) {
                    putValueArgument(index, IrGetValueImpl(parameter.startOffset, parameter.endOffset, parameter.symbol))
                }
            }

            statements += IrReturnImpl(newFunction.startOffset, newFunction.endOffset, irFunction.symbol, delegateCall)
        }

        return newFunction
    }

    private fun <T : IrElement> mapParameters(element: T, f: (IrValueSymbol) -> IrValueSymbol?): T {
        @Suppress("UNCHECKED_CAST")
        return element.transform(object : IrElementTransformer<Unit> {
            override fun visitElement(element: IrElement, data: Unit): IrElement {
                element.transformChildren(this, Unit)
                return element
            }

            override fun visitGetValue(expression: IrGetValue, data: Unit): IrExpression =
                    f(expression.symbol)?.let {
                        IrGetValueImpl(expression.startOffset, expression.endOffset, it, expression.origin)
                    } ?: expression
        }, Unit) as T
    }

    private fun generateDefaultArgumentInitializer(irFunction: IrFunction): List<IrStatement> {
        val isUndefinedSymbol = IrSimpleFunctionSymbolImpl(jsBuiltIns.isUndefined)
        val additionalStatements = mutableListOf<IrStatement>()
        for (irParameter in irFunction.valueParameters) {
            val irDefaultValue = irParameter.defaultValue ?: continue

            val builder = IrBlockBuilder(
                    IrGeneratorContext(builtIns), Scope(irFunction.symbol),
                    irParameter.startOffset, irParameter.endOffset)


            additionalStatements += builder.block {
                + irIfThen(
                        condition = irCall(isUndefinedSymbol).apply {
                            putValueArgument(0, irGet(irParameter.symbol))
                        },
                        thenPart = irSetVar(irParameter.symbol, irDefaultValue.expression)
                )
            }
        }

        return additionalStatements
    }

    private fun FunctionDescriptor.generateDefaultsFunction(): IrFunction {
        val name = Name.identifier("$name\$default")
        val descriptor = SimpleFunctionDescriptorImpl.create(
                /* containingDeclaration = */ containingDeclaration,
                /* annotations           = */ annotations,
                /* name                  = */ name,
                /* kind                  = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                /* source                = */ source)

        val newTypeParameters = typeParameters.map {
            TypeParameterDescriptorImpl.createForFurtherModification(
                    /* containingDeclaration = */ descriptor,
                    /* annotations           = */ it.annotations,
                    /* reified               = */ it.isReified,
                    /* variance              = */ it.variance,
                    /* name                  = */ it.name,
                    /* index                 = */ it.index,
                    /* source                = */ it.source,
                    /* reportCycleError      = */ null,
                    /* supertypeLoopsChecker = */ SupertypeLoopChecker.EMPTY
            )
        }

        val typeParameterMap = typeParameters.zip(newTypeParameters).toMap()
        val substitution = object : TypeSubstitution() {
            override fun get(key: KotlinType): TypeProjection? {
                val paramDescriptor = key.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return null
                val newDescriptor = typeParameterMap[paramDescriptor] ?: return null
                return TypeProjectionImpl(newDescriptor.defaultType)
            }
        }
        val substitutor = substitution.buildSubstitutor()
        for ((typeParameter, newTypeParameter) in typeParameters.zip(newTypeParameters)) {
            for (upperBound in typeParameter.upperBounds) {
                newTypeParameter.addUpperBound(substitutor.substitute(upperBound, Variance.INVARIANT)!!)
            }
            newTypeParameter.setInitialized()
        }
        fun substitute(type: KotlinType) = substitutor.substitute(type, Variance.INVARIANT)!!

        descriptor.initialize(
                /* receiverParameterType         = */ extensionReceiverParameter?.type,
                /* dispatchReceiverParameter     = */ dispatchReceiverParameter,
                /* typeParameters                = */ newTypeParameters,
                /* unsubstitutedValueParameters  = */ valueParameters.map {
            ValueParameterDescriptorImpl(
                    containingDeclaration = descriptor,
                    original              = null, /* ValueParameterDescriptorImpl::copy do not save original. */
                    index                 = it.index,
                    annotations           = it.annotations,
                    name                  = it.name,
                    outType               = substitute(it.type),
                    declaresDefaultValue  = false,
                    isCrossinline         = it.isCrossinline,
                    isNoinline            = it.isNoinline,
                    varargElementType     = it.varargElementType?.let { substitute(it) },
                    source                = it.source)
        },
                /* unsubstitutedReturnType       = */ returnType?.let { substitute(it) },
                /* modality                      = */ Modality.FINAL,
                /* visibility                    = */ this.visibility)
        descriptor.isSuspend = this.isSuspend

        val startOffset = this.startOffsetOrUndefined
        val endOffset = this.endOffsetOrUndefined

        val result: IrFunction = IrFunctionImpl(
            startOffset, endOffset,
            DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
            descriptor)

        result.createParameterDeclarations()

        return result
    }
}

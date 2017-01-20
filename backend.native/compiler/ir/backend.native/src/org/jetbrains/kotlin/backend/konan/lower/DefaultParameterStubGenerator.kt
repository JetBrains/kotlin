package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createFunctionIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.singletonList

class DefaultParameterStubGenerator internal constructor(val context: Context): DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction)
                lower(memberDeclaration)
            else
                null
        }

    }

    object DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER :
            IrDeclarationOriginImpl("DEFAULT_PARAMETER_EXTENT")

    private fun lower(irFunction: IrFunction): List<IrFunction> {
        val bodies = mutableListOf<IrExpressionBody>()
        irFunction.acceptChildrenVoid(object:IrElementVisitorVoid{
            override fun visitExpressionBody(body: IrExpressionBody) {
                bodies.add(body)
            }

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }
        })

        val functionDescriptor = irFunction.descriptor
        if (bodies.isNotEmpty()) {
            val (descriptor, mask, extension, dispatch) = functionDescriptor.generateDefaultsDescriptor()
            val builder = context.createFunctionIrBuilder(descriptor)
            val body = builder.irBlockBody(irFunction) {
                val params = mutableListOf<VariableDescriptor>()
                val variables = mutableMapOf<VariableDescriptor, VariableDescriptor>()

                for (valueParameter in functionDescriptor.valueParameters) {
                    val parameterDescriptor = descriptor.valueParameters[valueParameter.index]
                    if (valueParameter.hasDefaultValue()) {
                        val variable = scope.createTemporaryVariable(
                                irExpression = nullConst(valueParameter.type)!!,
                                isMutable = true)
                        val variableDescriptor = variable.descriptor
                        params.add(variableDescriptor)
                        +variable
                        val condition = irNotEquals(irCall(intAnd).apply {
                            dispatchReceiver = irGet(mask)
                            putValueArgument(0, irInt(1 shl valueParameter.index))
                        }, irInt(0))
                        val exprBody = getDefaultParameterExpressionBody(irFunction, valueParameter)
                        /* Use previously calculated values in next expression. */
                        exprBody.expression.transformChildrenVoid(object:IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                if (!variables.containsKey(expression.descriptor))
                                    return expression
                                return irGet(variables[expression.descriptor] as VariableDescriptor)
                            }
                        })
                        /* Mapping calculated values with its origin variables. */
                        variables.put(valueParameter, variableDescriptor)
                        +irIfThenElse(
                                type = KonanPlatform.builtIns.unitType,
                                condition = condition,
                                thenPart = irSetVar(variableDescriptor, exprBody.expression),
                                elsePart = irSetVar(variableDescriptor, irGet(parameterDescriptor)))

                    } else {
                        params.add(parameterDescriptor)
                    }
                }
                + irReturn(irCall(functionDescriptor).apply {
                    if (functionDescriptor.dispatchReceiverParameter != null) {
                        dispatchReceiver = irGet(dispatch!!)
                    }
                    if (functionDescriptor.extensionReceiverParameter != null) {
                        extensionReceiver = irGet(extension!!)
                    }
                    params.forEachIndexed { i, variable ->
                        putValueArgument(i, irGet(variable))
                    }
                })
            }
            // TODO: replace irFunction with new one without expression bodies.
            return listOf(irFunction, IrFunctionImpl(
                    irFunction.startOffset ,
                    irFunction.endOffset,
                    DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
                    descriptor, body))
        }
        return irFunction.singletonList()
    }
}

private fun getDefaultParameterExpressionBody(irFunction: IrFunction, valueParameter: ValueParameterDescriptor):IrExpressionBody {
    return irFunction.getDefault(valueParameter) as? IrExpressionBody ?: TODO("FIXME!!!")
}

private fun nullConst(type: KotlinType): IrExpression? {
    when {
        KotlinBuiltIns.isFloat(type)   -> return IrConstImpl<Float>   (0, 0, type, IrConstKind.Float,   0.0F)
        KotlinBuiltIns.isDouble(type)  -> return IrConstImpl<Double>  (0, 0, type, IrConstKind.Double,  0.0)
        KotlinBuiltIns.isBoolean(type) -> return IrConstImpl<Boolean> (0, 0, type, IrConstKind.Boolean, false)
        KotlinBuiltIns.isByte(type)    -> return IrConstImpl<Byte>    (0, 0, type, IrConstKind.Byte,    0)
        KotlinBuiltIns.isShort(type)   -> return IrConstImpl<Short>   (0, 0, type, IrConstKind.Short,   0)
        KotlinBuiltIns.isInt(type)     -> return IrConstImpl<Int>     (0, 0, type, IrConstKind.Int,     0)
        KotlinBuiltIns.isLong(type)    -> return IrConstImpl<Long>    (0, 0, type, IrConstKind.Long,    0)
        else                           -> return IrConstImpl<Nothing?>(0, 0, type, IrConstKind.Null,    null)
    }
}

class DefaultParameterInjector internal constructor(val context: Context): BodyLoweringPass {
    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                val descriptor = expression.descriptor
                if (descriptor.valueParameters.none{it.hasDefaultValue()})
                    return expression
                var argumentsCount = 0
                expression.acceptChildrenVoid(object:IrElementVisitorVoid{
                    override fun visitElement(element: IrElement) {
                        argumentsCount++
                    }
                })
                if (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null)
                    argumentsCount--
                if (argumentsCount == descriptor.valueParameters.size)
                    return expression
                var maskValue = 0
                val functionDescriptor = descriptor as FunctionDescriptor
                val (defaultFunctiondescriptor, mask, extension, dispatch) = functionDescriptor.generateDefaultsDescriptor()
                val params = descriptor.valueParameters.mapIndexed { i, it ->
                    if (expression.getValueArgument(i) == null) maskValue = maskValue or (1 shl i)
                    val valueParameterDescriptor = defaultFunctiondescriptor.valueParameters[i]
                    return@mapIndexed valueParameterDescriptor to (expression.getValueArgument(i) ?: nullConst(valueParameterDescriptor.type))
                } + (mask to IrConstImpl<Int>(
                        startOffset = irBody.startOffset,
                        endOffset   = irBody.endOffset,
                        type        = KonanPlatform.builtIns.intType,
                        kind        = IrConstKind.Int,
                        value       = maskValue))
                return IrCallImpl(
                        startOffset   = irBody.startOffset,
                        endOffset     =  irBody.endOffset,
                        type          = descriptor.returnType!!,
                        descriptor    = defaultFunctiondescriptor,
                        typeArguments = null)
                        .apply {
                            params.forEach {
                                putValueArgument(it.first.index, it.second)
                            }
                            extension?.apply {
                                putValueArgument(extension.index, expression.extensionReceiver)
                            }
                            dispatch?.apply {
                                putValueArgument(dispatch.index, expression.dispatchReceiver)
                            }
                        }
            }
        })
    }

}

val intDesctiptor = DescriptorUtils.getClassDescriptorForType(KonanPlatform.builtIns.intType)
val intAnd = DescriptorUtils.getFunctionByName(intDesctiptor.unsubstitutedMemberScope, Name.identifier("and"))

data class DefaultParameterDescriptor(val function: FunctionDescriptor, val mask:ValueParameterDescriptor,
                                      val extensionReceiver:ValueParameterDescriptor?, val dispatchReceiver: ValueParameterDescriptor?)

fun FunctionDescriptor.generateDefaultsDescriptor():DefaultParameterDescriptor {
    val name = Name.identifier("${this.name.asString()}\$default")
    val descriptor = SimpleFunctionDescriptorImpl.create(this.containingDeclaration, Annotations.EMPTY,
            name, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)
    val maskVariable = valueParameter(descriptor, valueParameters.size, "__\$mask\$__", KonanPlatform.builtIns.intType)

    var extensionReceiver:ValueParameterDescriptor? = null
    var index = valueParameters.size + 1
    extensionReceiverParameter?.let {
        extensionReceiver = valueParameter(descriptor, index++, "__\$ext_receiver\$__", extensionReceiverParameter!!.type)
    }
    var dispatchReceiver:ValueParameterDescriptor? = null
    dispatchReceiverParameter?.let {
        dispatchReceiver = valueParameter(descriptor, index++, "__\$dispatch_receiver\$__", dispatchReceiverParameter!!.type)
    }

    val parameterList = mutableListOf(*valueParameters.map{
        if (it.hasDefaultValue()) ValueParameterDescriptorImpl(
                containingDeclaration = it.containingDeclaration,
                original              = it.original,
                index                 = it.index,
                annotations           = it.annotations,
                name                  = it.name,
                outType               = it.type,
                declaresDefaultValue  = false,
                isCrossinline         = it.isCrossinline,
                isNoinline            = it.isNoinline,
                varargElementType     = it.varargElementType,
                source                = it.source)
        else it
    }.toTypedArray())

    parameterList.add(maskVariable)
    if (extensionReceiver != null) parameterList.add(extensionReceiver)
    if (dispatchReceiver != null) parameterList.add(dispatchReceiver)
    descriptor.initialize(
            /* receiverParameterType         = */ null,
            /* dispatchReceiverParameterType = */ null,
            /* typeParameters                = */ typeParameters,
            /* unsubstitutedValueParameters  = */ parameterList,
            /* unsubstitutedReturnType       = */ returnType,
            /* modality                      = */ this.modality,
            /* visibility                    = */ this.visibility)
    return DefaultParameterDescriptor(descriptor, maskVariable, extensionReceiver, dispatchReceiver)
}

private fun valueParameter(descriptor: FunctionDescriptor, index: Int, name: String, type: KotlinType):ValueParameterDescriptor {
     return ValueParameterDescriptorImpl(
            containingDeclaration = descriptor,
            original              =  null,
            index                 = index,
            annotations           = Annotations.EMPTY,
            name                  = Name.identifier(name),
            outType               = type,
            declaresDefaultValue  = false,
            isCrossinline         = false,
            isNoinline            = false,
            varargElementType     = null,
            source                = SourceElement.NO_SOURCE
    )
}
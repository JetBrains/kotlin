package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createFunctionIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.backend.konan.ir.ir2string
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
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
        val functionDescriptor = irFunction.descriptor

        if (hasNotDefaultParameters(functionDescriptor))
            return irFunction.singletonList()

        irFunction.acceptChildrenVoid(object:IrElementVisitorVoid{
            override fun visitExpressionBody(body: IrExpressionBody) {
                bodies.add(body)
            }

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }
        })

        log("detected ${functionDescriptor.name.asString()} has got #${bodies.size} default expressions")
        functionDescriptor.overriddenDescriptors.forEach { context.log("DEFAULT-REPLACER: $it") }
        if (bodies.isNotEmpty()) {
            val description = functionDescriptor.getOrCreateDefaultsDescription(context)
            val builder = context.createFunctionIrBuilder(description.function)
            val body = builder.irBlockBody(irFunction) {
                val params = mutableListOf<VariableDescriptor>()
                val variables = mutableMapOf<VariableDescriptor, VariableDescriptor>()

                val newExtensionReceiver =
                        if (description.function.extensionReceiverParameter == null) {
                            null
                        } else {
                            IrGetValueImpl(
                                    startOffset = irFunction.startOffset,
                                    endOffset = irFunction.endOffset,
                                    descriptor = description.function.extensionReceiverParameter!!,
                                    origin = null
                            )
                        }

                for (valueParameter in functionDescriptor.valueParameters) {
                    val parameterDescriptor = description.function.valueParameters[valueParameter.index]
                    if (valueParameter.hasDefaultValue()) {
                        val variable = scope.createTemporaryVariable(
                                irExpression = nullConst(valueParameter.type)!!,
                                isMutable = true)
                        val variableDescriptor = variable.descriptor
                        params.add(variableDescriptor)
                        +variable
                        val condition = irNotEquals(irCall(intAnd).apply {
                            dispatchReceiver = irGet(description.mask)
                            putValueArgument(0, irInt(1 shl valueParameter.index))
                        }, irInt(0))
                        val exprBody = getDefaultParameterExpressionBody(irFunction, valueParameter)
                        /* Use previously calculated values in next expression. */
                        exprBody.expression.transformChildrenVoid(object:IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                if (expression.descriptor == functionDescriptor.extensionReceiverParameter)
                                    return newExtensionReceiver!!
                                if (!variables.containsKey(expression.descriptor))
                                    return expression
                                return irGet(variables[expression.descriptor] as VariableDescriptor)
                            }
                        })
                        /* Mapping calculated values with its origin variables. */
                        variables.put(valueParameter, variableDescriptor)
                        +irIfThenElse(
                                type      = KonanPlatform.builtIns.unitType,
                                condition = condition,
                                thenPart  = irSetVar(variableDescriptor, exprBody.expression),
                                elsePart  = irSetVar(variableDescriptor, irGet(parameterDescriptor)))
                    } else {
                        params.add(parameterDescriptor)
                    }
                }
                if (functionDescriptor !is ClassConstructorDescriptor) {
                    +irReturn(irCall(functionDescriptor).apply {
                        if (functionDescriptor.dispatchReceiverParameter != null) {
                            dispatchReceiver = irThis()
                        }
                        if (functionDescriptor.extensionReceiverParameter != null) {
                            extensionReceiver = newExtensionReceiver
                        }
                        params.forEachIndexed { i, variable ->
                            putValueArgument(i, irGet(variable))
                        }
                    })
                } else {
                    + irReturn(IrDelegatingConstructorCallImpl(
                            startOffset = irFunction.startOffset,
                            endOffset   = irFunction.endOffset,
                            descriptor  = functionDescriptor
                    ).apply {
                        params.forEachIndexed { i, variable ->
                            putValueArgument(i, irGet(variable))
                        }
                    })
                }
            }
            // TODO: replace irFunction with new one without expression bodies.
            return listOf(irFunction, IrFunctionImpl(
                    irFunction.startOffset,
                    irFunction.endOffset,
                    DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER,
                    description.function, body))
        }
        return irFunction.singletonList()
    }

    private fun log(msg:String) = context.log("DEFAULT-REPLACER: $msg")
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
            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                super.visitDelegatingConstructorCall(expression)
                val descriptor = expression.descriptor
                if (hasNotDefaultParameters(descriptor))
                    return expression
                val argumentsCount = argumentCount(expression)
                if (argumentsCount == descriptor.valueParameters.size)
                    return expression
                val (desc, params) = parametersForCall(expression)
                return IrDelegatingConstructorCallImpl(
                        startOffset   = irBody.startOffset,
                        endOffset     = irBody.endOffset,
                        descriptor    = desc.function as ClassConstructorDescriptor)
                        .apply {
                            params.forEach {
                                log("call::params@${it.first.index}/${it.first.name.asString()}: ${ir2string(it.second)}")
                                putValueArgument(it.first.index, it.second)
                            }
                        }
            }

            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                val functionDescriptor = expression.descriptor as FunctionDescriptor

                if (hasNotDefaultParameters(functionDescriptor))
                    return expression

                val argumentsCount = argumentCount(expression)
                if (argumentsCount == functionDescriptor.valueParameters.size)
                    return expression
                val (desc, params) = parametersForCall(expression)
                return IrCallImpl(
                        startOffset   = irBody.startOffset,
                        endOffset     = irBody.endOffset,
                        type          = desc.function.returnType!!,
                        descriptor    = desc.function,
                        typeArguments = null)
                        .apply {
                            params.forEach {
                                log("call::params@${it.first.index}/${it.first.name.asString()}: ${ir2string(it.second)}")
                                putValueArgument(it.first.index, it.second)
                            }
                            expression.extensionReceiver?.apply{
                                extensionReceiver = expression.extensionReceiver
                            }
                            expression.dispatchReceiver?.apply {
                                dispatchReceiver = expression.dispatchReceiver
                            }
                            log("call::extension@: ${ir2string(expression.extensionReceiver)}")
                            log("call::dispatch@: ${ir2string(expression.dispatchReceiver)}")
                        }
            }

            private fun parametersForCall(expression: IrMemberAccessExpression): Pair<DefaultParameterDescription, List<Pair<ValueParameterDescriptor, IrExpression?>>> {
                var maskValue = 0
                val rawDescriptor = expression.descriptor as FunctionDescriptor
                val descriptor =  rawDescriptor.overriddenDescriptors.firstOrNull()?:rawDescriptor
                val desc = descriptor.getOrCreateDefaultsDescription(context)
                descriptor.valueParameters.forEach {
                    log("descriptor::${descriptor.name.asString()}#${it.index}: ${it.name.asString()}")
                }
                val params = descriptor.valueParameters.mapIndexed { i, _ ->
                    val valueArgument = expression.getValueArgument(i)
                    if (valueArgument == null) maskValue = maskValue or (1 shl i)
                    val valueParameterDescriptor = desc.function.valueParameters[i]
                    val pair = valueParameterDescriptor to (valueArgument ?: nullConst(valueParameterDescriptor.type))
                    return@mapIndexed pair
                } + (desc.mask to IrConstImpl<Int>(
                        startOffset = irBody.startOffset,
                        endOffset = irBody.endOffset,
                        type = KonanPlatform.builtIns.intType,
                        kind = IrConstKind.Int,
                        value = maskValue))
                params.forEach {
                    log("descriptor::${desc.function.name.asString()}#${it.first.index}: ${it.first.name.asString()}")
                }
                return Pair(desc, params)
            }

            private fun argumentCount(expression: IrMemberAccessExpression): Int {
                var argumentsCount = 0
                expression.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        argumentsCount++
                    }
                })
                val descriptor = expression.descriptor
                if (descriptor !is ConstructorDescriptor && (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null))
                    argumentsCount--
                return argumentsCount
            }
        })
    }

    private fun log(msg: String) = context.log("DEFAULT-INJECTOR: $msg")
}

private fun hasNotDefaultParameters(descriptor: CallableDescriptor) = descriptor.valueParameters.none { it.hasDefaultValue() }

val intDesctiptor = DescriptorUtils.getClassDescriptorForType(KonanPlatform.builtIns.intType)
val intAnd = DescriptorUtils.getFunctionByName(intDesctiptor.unsubstitutedMemberScope, Name.identifier("and"))

data class DefaultParameterDescription(val function: FunctionDescriptor, val mask:ValueParameterDescriptor,
                                       val hasExtensionReceiver:Boolean, val hasDispatchReceiver: Boolean)

private fun FunctionDescriptor.getOrCreateDefaultsDescription(context: Context): DefaultParameterDescription {
    return context.ir.defaultParameterDescriptions.getOrPut(this) {
        this.generateDefaultsDescription(context)
    }
}

private fun FunctionDescriptor.generateDefaultsDescription(context: Context): DefaultParameterDescription {
    val descriptor = when (this){
        is ConstructorDescriptor -> DefaultParameterClassConstructorDescriptor(
                containingDeclaration = this.containingDeclaration as ClassDescriptor,
                annotations = annotations,
                original = null,
                isPrimary = false,
                source = SourceElement.NO_SOURCE,
                kind = CallableMemberDescriptor.Kind.SYNTHESIZED)

        is FunctionDescriptor    -> SimpleFunctionDescriptorImpl.create(
                                     /* containingDeclaration = */ this.containingDeclaration,
                                     /* annotations           = */ Annotations.EMPTY,
                                     /* name                  = */ name,
                                     /* kind                  = */ CallableMemberDescriptor.Kind.SYNTHESIZED,
                                     /* source                = */ SourceElement.NO_SOURCE)
        else                     -> TODO("FIXME!!!")
    }

    val parameterList = mutableListOf<ValueParameterDescriptor>()

    val maskVariable = valueParameter(descriptor, valueParameters.size , "__\$mask\$__", KonanPlatform.builtIns.intType)

    parameterList += valueParameters.map{
        ValueParameterDescriptorImpl(
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
    }

    parameterList.add(maskVariable)
    descriptor.initialize(
            /* receiverParameterType         = */ extensionReceiverParameter?.type,
            /* dispatchReceiverParameter     = */ dispatchReceiverParameter,
            /* typeParameters                = */ typeParameters,
            /* unsubstitutedValueParameters  = */ parameterList,
            /* unsubstitutedReturnType       = */ returnType,
            /* modality                      = */ Modality.FINAL,
            /* visibility                    = */ this.visibility)
    return DefaultParameterDescription(
            function             = descriptor,
            mask                 = maskVariable,
            hasDispatchReceiver  = (extensionReceiverParameter != null),
            hasExtensionReceiver = (dispatchReceiverParameter  != null))
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

/**
 *  This descriptor overrides name property, for class constructor : instead of <init> -> <init>$defeult symbol.
 */
class DefaultParameterClassConstructorDescriptor(
        containingDeclaration: ClassDescriptor,
        original: ConstructorDescriptor?,
        annotations: Annotations,
        isPrimary: Boolean,
        kind: CallableMemberDescriptor.Kind,
        source: SourceElement
) : ClassConstructorDescriptorImpl(containingDeclaration, original, annotations, isPrimary, kind, source) {

    override fun getName(): Name {
        return Name.identifier("${super.getName().asString()}\$default")
    }
}
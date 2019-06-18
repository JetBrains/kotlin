package androidx.compose.plugins.kotlin.compiler.lower

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import androidx.compose.plugins.kotlin.GeneratedKtxChildrenLambdaClassDescriptor
import androidx.compose.plugins.kotlin.compiler.ir.buildWithScope
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.KotlinType

fun generateChildrenLambda(
    context: GeneratorContext,
    container: IrPackageFragment,
    capturedAccesses: List<IrDeclarationReference>,
    functionType: KotlinType,
    parameters: List<ValueParameterDescriptor>,
    body: Collection<IrStatement>
): IrClass {

    val syntheticClassDescriptor =
        GeneratedKtxChildrenLambdaClassDescriptor(
            context.moduleDescriptor,
            container.packageFragmentDescriptor,
            capturedAccesses.map { it.type.toKotlinType() },
            functionType,
            parameters
        )
    val lambdaClass = context.symbolTable.declareClass(
        -1,
        -1,
        IrDeclarationOrigin.DEFINED,
        syntheticClassDescriptor
    )
    lambdaClass.thisReceiver = context.symbolTable.declareValueParameter(
        -1, -1,
        IrDeclarationOrigin.INSTANCE_RECEIVER,
        syntheticClassDescriptor.thisAsReceiverParameter,
        syntheticClassDescriptor.thisAsReceiverParameter.type.toIrType()!!
    )

    syntheticClassDescriptor.capturedAccessesAsProperties.forEach {
        lambdaClass.declarations.add(
            context.symbolTable.declareField(
                it.startOffset ?: -1,
                it.endOffset ?: -1,
                IrDeclarationOrigin.DEFINED,
                it,
                it.type.toIrType()!!
            )
        )
    }
    lambdaClass.declarations.add(
        generateConstructor(
            context,
            syntheticClassDescriptor
        )
    )
    lambdaClass.declarations.add(
        generateInvokeFunction(
            context,
            syntheticClassDescriptor,
            capturedAccesses,
            parameters,
            body
        )
    )
    // TODO: generate an equals function

    return lambdaClass
}

private fun generateConstructor(
    context: GeneratorContext,
    syntheticClassDescriptor: GeneratedKtxChildrenLambdaClassDescriptor
): IrConstructor {
    return context.symbolTable.declareConstructor(
        -1,
        -1,
        IrDeclarationOrigin.DEFINED,
        syntheticClassDescriptor.unsubstitutedPrimaryConstructor
    )
        .buildWithScope(context) { constructor ->
            val lambdaAsThisReceiver = context.symbolTable.referenceValueParameter(
                syntheticClassDescriptor.thisAsReceiverParameter
            )

            val irValueParameters =
                syntheticClassDescriptor.unsubstitutedPrimaryConstructor.valueParameters.map {
                    context.symbolTable.declareValueParameter(
                        -1,
                        -1,
                        IrDeclarationOrigin.DEFINED,
                        it,
                        it.type.toIrType()!!)
                }
            constructor.valueParameters.addAll(irValueParameters)

            val getThisExpr = IrGetValueImpl(-1, -1, lambdaAsThisReceiver)

            val statements = mutableListOf<IrStatement>()
            val superConstructor =
                context.symbolTable.referenceConstructor(
                    syntheticClassDescriptor.getSuperClassOrAny().constructors.single {
                        it.valueParameters.size == 0
                    }
                )
            val constructorSymbol =
                context.symbolTable.referenceConstructor(superConstructor.descriptor.original)
            val superCall =
                IrDelegatingConstructorCallImpl(
                    -1,
                    -1,
                    superConstructor.descriptor.returnType.toIrType()!!,
                    constructorSymbol,
                    superConstructor.descriptor
                )

            statements.add(superCall)

            statements.add(
                IrInstanceInitializerCallImpl(
                    -1,
                    -1,
                    context.symbolTable.referenceClass(syntheticClassDescriptor),
                    syntheticClassDescriptor.defaultType.toIrType()!!
                )
            )

            constructor.valueParameters.forEachIndexed { index, irValueParameter ->
                val propertyDescriptor =
                    syntheticClassDescriptor.capturedAccessesAsProperties[index]
                val fieldSymbol = context.symbolTable.referenceField(propertyDescriptor)
                statements.add(
                    IrSetFieldImpl(
                        -1,
                        -1,
                        fieldSymbol,
                        getThisExpr,
                        IrGetValueImpl(-1, -1, irValueParameter.symbol),
                        propertyDescriptor.type.toIrType()!!
                    )
                )
            }

            constructor.body = IrBlockBodyImpl(-1, -1, statements)
            constructor.returnType =
                syntheticClassDescriptor.unsubstitutedPrimaryConstructor.returnType.toIrType()!!
        }
}

private fun generateInvokeFunction(
    context: GeneratorContext,
    syntheticClassDescriptor: GeneratedKtxChildrenLambdaClassDescriptor,
    capturedAccesses: List<IrDeclarationReference>,
    parameters: List<ValueParameterDescriptor>,
    body: Collection<IrStatement>
): IrFunction {

    val functionDescriptor = syntheticClassDescriptor.invokeDescriptor
    return context.symbolTable.declareSimpleFunction(
        -1,
        -1,
        IrDeclarationOrigin.DEFINED,
        functionDescriptor
    )
        .buildWithScope(context) { irFunction ->

    val lambdaAsThisReceiver = context.symbolTable.referenceValueParameter(
        syntheticClassDescriptor.thisAsReceiverParameter
    )

    irFunction.dispatchReceiverParameter =
        context.symbolTable.declareValueParameter(
            -1,
            -1,
            IrDeclarationOrigin.DEFINED,
            functionDescriptor.dispatchReceiverParameter!!,
            functionDescriptor.dispatchReceiverParameter!!.type.toIrType()!!
        )

    val irValueParameters =
        functionDescriptor.valueParameters.map {
            context.symbolTable.declareValueParameter(
                -1,
                -1,
                IrDeclarationOrigin.DEFINED,
                it,
                it.type.toIrType()!!)
        }

    val transformedBody = body.map {
        it.transform(object : IrElementTransformer<Nothing?> {

            override fun visitGetValue(expression: IrGetValue, data: Nothing?): IrExpression {
                return when {
                    expression in capturedAccesses -> {
                        val descriptor =
                            syntheticClassDescriptor.capturedAccessesAsProperties[
                                    capturedAccesses.indexOf(expression)
                            ]
                        val newSymbol = context.symbolTable.referenceField(
                            descriptor
                        )
                        IrGetFieldImpl(
                            expression.startOffset,
                            expression.endOffset,
                            newSymbol,
                            descriptor.type.toIrType()!!,
                            IrGetValueImpl(-1, -1, lambdaAsThisReceiver),
                            expression.origin
                        )
                    }
                    expression.symbol.descriptor in parameters -> {
                        val index = parameters.indexOf(expression.symbol.descriptor)
                        val newSymbol = context.symbolTable.referenceValueParameter(
                            syntheticClassDescriptor.invokeDescriptor.valueParameters[index]
                        )
                        IrGetValueImpl(
                            expression.startOffset,
                            expression.endOffset,
                            newSymbol,
                            expression.origin
                        )
                    }
                    else -> expression
                }
            }

            override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?): IrElement {
                expression.transformChildren(this, data)
                return expression
            }
        }, null)
    }

            irFunction.valueParameters.addAll(irValueParameters)
            irFunction.returnType = functionDescriptor.returnType!!.toIrType()!!
            irFunction.body = IrBlockBodyImpl(-1, -1, transformedBody.toList())
        }
}

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.OverriddenFunctionDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor

internal class ClassDelegationLowering(val context: Context) : DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat {
            when (it) {
                is IrFunction -> {
                    val transformedFun = transformBridgeToDelegatedMethod(irDeclarationContainer, it)
                    if (transformedFun == null) null
                    else listOf(transformedFun)
                }
                is IrProperty -> {
                    val getter = transformBridgeToDelegatedMethod(irDeclarationContainer, it.getter)
                    val setter = transformBridgeToDelegatedMethod(irDeclarationContainer, it.setter)
                    if (getter != null) it.getter = getter
                    if (setter != null) it.setter = setter
                    null
                }
                else -> null
            }
        }
    }

    private fun transformBridgeToDelegatedMethod(irDeclarationContainer: IrDeclarationContainer, irFunction: IrFunction?): IrFunction? {
        if (irFunction == null) return null
        val descriptor = irFunction.descriptor

        if (descriptor.kind != CallableMemberDescriptor.Kind.DELEGATION) return null

        // TODO: hack because of broken IR for synthesized delegated members: https://youtrack.jetbrains.com/issue/KT-16486.
        val body = irFunction.body as? IrBlockBody
                ?: throw AssertionError("Unexpected method body: ${irFunction.body}")
        val statement = body.statements.single()
        val delegatedCall = ((statement as? IrReturn)?.value ?: statement) as? IrCall
                ?: throw AssertionError("Unexpected method body: $statement")
        val propertyGetter = delegatedCall.dispatchReceiver as? IrGetValue
                ?: throw AssertionError("Unexpected dispatch receiver: ${delegatedCall.dispatchReceiver}")
        val propertyDescriptor = propertyGetter.descriptor as? PropertyDescriptor
                ?: throw AssertionError("Unexpected dispatch receiver descriptor: ${propertyGetter.descriptor}")
        val delegated = context.specialDescriptorsFactory.getBridgeDescriptor(
                OverriddenFunctionDescriptor(descriptor, delegatedCall.descriptor as FunctionDescriptor))
        val newFunction = IrFunctionImpl(irFunction.startOffset, irFunction.endOffset, irFunction.origin, delegated)

        val irBlockBody = IrBlockBodyImpl(irFunction.startOffset, irFunction.endOffset)
        val returnType = delegatedCall.descriptor.returnType!!
        val irCall = IrCallImpl(irFunction.startOffset, irFunction.endOffset, returnType, delegatedCall.descriptor, null)
        val receiver = IrGetValueImpl(irFunction.startOffset, irFunction.endOffset,
                (irDeclarationContainer as IrClass).descriptor.thisAsReceiverParameter)
        irCall.dispatchReceiver = IrGetFieldImpl(irFunction.startOffset, irFunction.endOffset, propertyDescriptor, receiver)
        irCall.extensionReceiver = delegated.extensionReceiverParameter?.let { extensionReceiver ->
            IrGetValueImpl(irFunction.startOffset, irFunction.endOffset, extensionReceiver)
        }
        irCall.mapValueParameters { overriddenValueParameter ->
            val delegatedValueParameter = delegated.valueParameters[overriddenValueParameter.index]
            IrGetValueImpl(irFunction.startOffset, irFunction.endOffset, delegatedValueParameter)
        }
        if (KotlinBuiltIns.isUnit(returnType) || KotlinBuiltIns.isNothing(returnType)) {
            irBlockBody.statements.add(irCall)
        } else {
            val irReturn = IrReturnImpl(irFunction.startOffset, irFunction.endOffset, context.builtIns.nothingType, delegated, irCall)
            irBlockBody.statements.add(irReturn)
        }

        newFunction.body = irBlockBody
        return newFunction
    }
}

internal class PropertyDelegationLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                return super.visitFunction(declaration)
            }

            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
                declaration.transformChildrenVoid(this)
                val name = declaration.descriptor.name.asString()
                val type = declaration.descriptor.type

                val initializer = declaration.delegate.initializer!!
                return IrVariableImpl(declaration.startOffset, declaration.endOffset,
                        declaration.origin, declaration.delegate.descriptor,
                        IrBlockImpl(initializer.startOffset, initializer.endOffset, initializer.type, null,
                                listOf(
                                        transformBridgeToDelegate(name, type, declaration.getter),
                                        transformBridgeToDelegate(name, type, declaration.setter),
                                        initializer
                                ).filterNotNull())
                )

            }

            override fun visitProperty(declaration: IrProperty): IrStatement {
                declaration.transformChildrenVoid(this)
                if (declaration.isDelegated) {
                    val name = declaration.descriptor.name.asString()
                    val type = declaration.descriptor.returnType!!
                    declaration.getter = transformBridgeToDelegate(name, type, declaration.getter)
                    declaration.setter = transformBridgeToDelegate(name, type, declaration.setter)
                }
                return declaration
            }

            private val kotlinPackage = context.irModule!!.descriptor.getPackage(FqName.fromSegments(listOf("kotlin", "reflect")))
            private val genericKPropertyImplType = kotlinPackage.memberScope.getContributedClassifier(Name.identifier("KPropertyImpl"),
                    NoLookupLocation.FROM_BACKEND) as ClassDescriptor

            private fun transformBridgeToDelegate(name: String, type: KotlinType, irFunction: IrFunction?): IrFunction? {
                irFunction?.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitCallableReference(expression: IrCallableReference): IrExpression {
                        val typeParameterT = genericKPropertyImplType.declaredTypeParameters[0]
                        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(type)))
                        val kPropertyImplType = genericKPropertyImplType.substitute(typeSubstitutor)
                        return IrCallImpl(expression.startOffset, expression.endOffset,
                                kPropertyImplType.defaultType, kPropertyImplType.unsubstitutedPrimaryConstructor!!, null).apply {
                            putValueArgument(0, IrConstImpl<String>(expression.startOffset, expression.endOffset,
                                    context.builtIns.stringType, IrConstKind.String, name))
                        }
                    }
                })
                return irFunction
            }
        })
    }
}


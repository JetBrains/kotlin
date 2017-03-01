package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.descriptors.initialize
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.OverriddenFunctionDescriptor
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.createArrayOfExpression
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*

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
        val delegated = context.specialDescriptorsFactory.getBridgeDescriptor(
                OverriddenFunctionDescriptor(descriptor, delegatedCall.descriptor as FunctionDescriptor))
        val newFunction = IrFunctionImpl(irFunction.startOffset, irFunction.endOffset, irFunction.origin, delegated)

        val irBlockBody = IrBlockBodyImpl(irFunction.startOffset, irFunction.endOffset)
        val returnType = delegatedCall.descriptor.returnType!!
        val irCall = IrCallImpl(irFunction.startOffset, irFunction.endOffset, returnType, delegatedCall.descriptor, null)
        irCall.dispatchReceiver = delegatedCall.dispatchReceiver
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
    private val kotlinReflectPackage = context.irModule!!.descriptor.getPackage(FqName.fromSegments(listOf("kotlin", "reflect")))
    private val genericKPropertyImplType = kotlinReflectPackage.memberScope.getContributedClassifier(Name.identifier("KPropertyImpl"),
            NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    private val kotlinPackage = context.irModule!!.descriptor.getPackage(FqName("kotlin"))
    private val genericArrayType = kotlinPackage.memberScope.getContributedClassifier(Name.identifier("Array"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    fun getKPropertyImplConstructorDescriptorWithProjection(type: KotlinType): ClassConstructorDescriptor {
        val typeParameterT = genericKPropertyImplType.declaredTypeParameters[0]
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(type)))
        return genericKPropertyImplType.unsubstitutedPrimaryConstructor!!.substitute(typeSubstitutor)!!
    }

    fun ClassDescriptor.replace(vararg type: KotlinType): SimpleType {
        return this.defaultType.replace(type.map(::TypeProjectionImpl))
    }

    override fun lower(irFile: IrFile) {
        val kProperties = mutableListOf<IrExpression>()

        val getter = genericArrayType.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("get"), NoLookupLocation.FROM_BACKEND).single()
        val typeParameterT = genericArrayType.declaredTypeParameters[0]
        val kPropertyImplType = genericKPropertyImplType.replace(context.builtIns.anyType)
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(kPropertyImplType)))
        val substitutedGetter = getter.substitute(typeSubstitutor)!!

        val kPropertiesField = createKPropertiesFieldDescriptor(irFile.packageFragmentDescriptor, genericArrayType.replace(kPropertyImplType))

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

            private fun transformBridgeToDelegate(name: String, type: KotlinType, irFunction: IrFunction?): IrFunction? {
                irFunction?.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitCallableReference(expression: IrCallableReference): IrExpression {
                        val fieldInitializer = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                getKPropertyImplConstructorDescriptorWithProjection(type)).apply {
                            putValueArgument(0, IrConstImpl<String>(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                    context.builtIns.stringType, IrConstKind.String, name))
                        }

                        val index = kProperties.size
                        kProperties.add(fieldInitializer)

                        return IrCallImpl(expression.startOffset, expression.endOffset, substitutedGetter).apply {
                            dispatchReceiver = IrGetFieldImpl(expression.startOffset, expression.endOffset, kPropertiesField)
                            putValueArgument(0, IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, index))
                        }
                    }
                })
                return irFunction
            }

        })

        if (kProperties.size > 0) {
            irFile.declarations.add(0, IrFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION,
                    kPropertiesField,
                    IrExpressionBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            context.createArrayOfExpression(kPropertyImplType, kProperties))))
        }
    }

    private object DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION :
            IrDeclarationOriginImpl("KPROPERTIES_FOR_DELEGATION")

    private fun createKPropertiesFieldDescriptor(containingDeclaration: DeclarationDescriptor, fieldType: SimpleType): PropertyDescriptorImpl {
        return PropertyDescriptorImpl.create(containingDeclaration, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE,
                false, "KPROPERTIES".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
                false, false, false, false, false, false).initialize(fieldType)
    }
}


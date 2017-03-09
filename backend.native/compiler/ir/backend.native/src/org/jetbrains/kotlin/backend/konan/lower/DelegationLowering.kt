package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.descriptors.initialize
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.createArrayOfExpression
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*

internal class PropertyDelegationLowering(val context: Context) : FileLoweringPass {
    private val genericKProperty0ImplType = context.reflectionTypes.kProperty0Impl
    private val genericKLocalDelegatedPropertyImplType = context.reflectionTypes.kLocalDelegatedPropertyImpl
    private val genericKProperty1ImplType = context.reflectionTypes.kProperty1Impl
    private val genericKMutableProperty0ImplType = context.reflectionTypes.kMutableProperty0Impl
    private val genericKMutableProperty1ImplType = context.reflectionTypes.kMutableProperty1Impl
    private val genericKLocalDelegatedMutablePropertyImplType = context.reflectionTypes.kLocalDelegatedMutablePropertyImpl

    private val kotlinPackage = context.irModule!!.descriptor.getPackage(FqName("kotlin"))
    private val genericArrayType = kotlinPackage.memberScope.getContributedClassifier(Name.identifier("Array"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    private fun getKPropertyImplConstructorDescriptor(returnType: KotlinType, isLocal: Boolean, isMutable: Boolean): ClassConstructorDescriptor {
        val genericKPropertyImplType =
                if (isMutable) {
                    if (isLocal) genericKLocalDelegatedMutablePropertyImplType else genericKMutableProperty0ImplType
                } else {
                    if (isLocal) genericKLocalDelegatedPropertyImplType else genericKProperty0ImplType
                }
        val typeParameterR = genericKPropertyImplType.declaredTypeParameters[0]
        val typeSubstitutor = TypeSubstitutor.create(mapOf(
                typeParameterR.typeConstructor to TypeProjectionImpl(returnType)))
        return genericKPropertyImplType.unsubstitutedPrimaryConstructor!!.substitute(typeSubstitutor)!!
    }

    private fun getKPropertyImplConstructorDescriptor(receiverType: KotlinType?, returnType: KotlinType, isLocal: Boolean, isMutable: Boolean)
            : ClassConstructorDescriptor {
        if (receiverType == null)
            return getKPropertyImplConstructorDescriptor(returnType, isLocal, isMutable)
        assert(!isLocal, { "Local delegated property always has implicit receiver" })
        val genericKPropertyImplType = if (isMutable) genericKMutableProperty1ImplType else genericKProperty1ImplType
        val typeParameterT = genericKPropertyImplType.declaredTypeParameters[0]
        val typeParameterR = genericKPropertyImplType.declaredTypeParameters[1]
        val typeSubstitutor = TypeSubstitutor.create(mapOf(
                typeParameterT.typeConstructor to TypeProjectionImpl(receiverType),
                typeParameterR.typeConstructor to TypeProjectionImpl(returnType)))
        return genericKPropertyImplType.unsubstitutedPrimaryConstructor!!.substitute(typeSubstitutor)!!
    }

    private fun ClassDescriptor.replace(vararg type: KotlinType): SimpleType {
        return this.defaultType.replace(type.map(::TypeProjectionImpl))
    }

    override fun lower(irFile: IrFile) {
        val kProperties = mutableMapOf<VariableDescriptorWithAccessors, Pair<IrExpression, Int>>()

        val arrayItemGetter = genericArrayType.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("get"),
                NoLookupLocation.FROM_BACKEND).single()
        val typeParameterT = genericArrayType.declaredTypeParameters[0]
        val kPropertyImplType = genericKProperty1ImplType.replace(context.builtIns.anyType, context.builtIns.anyType)
        val typeSubstitutor = TypeSubstitutor.create(mapOf(typeParameterT.typeConstructor to TypeProjectionImpl(kPropertyImplType)))
        val substitutedArrayItemGetter = arrayItemGetter.substitute(typeSubstitutor)!!

        val kPropertiesField = createKPropertiesFieldDescriptor(irFile.packageFragmentDescriptor, genericArrayType.replace(kPropertyImplType))

        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                return super.visitFunction(declaration)
            }

            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
                declaration.transformChildrenVoid(this)

                val initializer = declaration.delegate.initializer!!
                return IrVariableImpl(declaration.startOffset, declaration.endOffset,
                        declaration.origin, declaration.delegate.descriptor,
                        IrBlockImpl(initializer.startOffset, initializer.endOffset, initializer.type, null,
                                listOf(
                                        declaration.getter,
                                        declaration.setter,
                                        initializer
                                ).filterNotNull())
                )

            }

            override fun visitCallableReference(expression: IrCallableReference): IrExpression {
                expression.transformChildrenVoid(this)
                val propertyDescriptor = expression.descriptor as? VariableDescriptorWithAccessors
                if (propertyDescriptor == null) return expression
                val receiversCount = listOf(expression.dispatchReceiver, expression.extensionReceiver).count { it != null }
                if (receiversCount == 1 && propertyDescriptor is PropertyDescriptor) // Has receiver and is not local delegated.
                    return createKProperty(expression, propertyDescriptor)
                else if (receiversCount == 2)
                    throw AssertionError("Callable reference to properties with two receivers is not allowed: $propertyDescriptor")
                else { // Cache KProperties with no arguments.
                    val field = kProperties.getOrPut(propertyDescriptor) {
                        createKProperty(expression, propertyDescriptor) to kProperties.size
                    }

                    return IrCallImpl(expression.startOffset, expression.endOffset, substitutedArrayItemGetter).apply {
                        dispatchReceiver = IrGetFieldImpl(expression.startOffset, expression.endOffset, kPropertiesField)
                        putValueArgument(0, IrConstImpl.int(startOffset, endOffset, context.builtIns.intType, field.second))
                    }
                }

            }
        })

        if (kProperties.isNotEmpty()) {
            val initializers = kProperties.values.sortedBy { it.second }.map { it.first }
            // TODO: move to object for lazy initialization.
            irFile.declarations.add(0, IrFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION,
                    kPropertiesField,
                    IrExpressionBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            context.createArrayOfExpression(kPropertyImplType, initializers))))
        }
    }

    private fun createKProperty(expression: IrCallableReference, propertyDescriptor: VariableDescriptorWithAccessors): IrCallImpl {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        var receiverType: KotlinType? = null
        val isLocal = propertyDescriptor !is PropertyDescriptor

        val getterCallableReference = propertyDescriptor.getter.let {
            if (it == null || isLocal) null
            else {
                val getter = propertyDescriptor.getter!!
                val receiverTypes = mutableListOf<KotlinType>()
                getter.dispatchReceiverParameter.let {
                    if (it != null && expression.dispatchReceiver == null)
                        receiverTypes.add(it.type)
                }
                getter.extensionReceiverParameter.let {
                    if (it != null && expression.extensionReceiver == null)
                        receiverTypes.add(it.type)
                }
                receiverType = receiverTypes.singleOrNull()
                val getterKFunctionType = context.reflectionTypes.getKFunctionType(
                        annotations = Annotations.EMPTY,
                        receiverType = receiverType,
                        parameterTypes = listOf(),
                        returnType = propertyDescriptor.type)
                IrCallableReferenceImpl(startOffset, endOffset, getterKFunctionType, getter, null).apply {
                    dispatchReceiver = expression.dispatchReceiver
                    extensionReceiver = expression.extensionReceiver
                }
            }
        }

        val setterCallableReference = propertyDescriptor.setter.let {
            if (it == null || isLocal) null
            else {
                val setterKFunctionType = context.reflectionTypes.getKFunctionType(
                        annotations = Annotations.EMPTY,
                        receiverType = receiverType,
                        parameterTypes = listOf(propertyDescriptor.type),
                        returnType = context.builtIns.unitType)
                IrCallableReferenceImpl(startOffset, endOffset, setterKFunctionType, it, null).apply {
                    dispatchReceiver = expression.dispatchReceiver
                    extensionReceiver = expression.extensionReceiver
                }
            }
        }

        val descriptor = getKPropertyImplConstructorDescriptor(
                receiverType = receiverType,
                returnType = propertyDescriptor.type,
                isLocal = isLocal,
                isMutable = setterCallableReference != null)
        val initializer = IrCallImpl(startOffset, endOffset, descriptor).apply {
            putValueArgument(0, IrConstImpl<String>(startOffset, endOffset,
                    context.builtIns.stringType, IrConstKind.String, propertyDescriptor.name.asString()))
            if (getterCallableReference != null)
                putValueArgument(1, getterCallableReference)
            if (setterCallableReference != null)
                putValueArgument(2, setterCallableReference)
        }
        return initializer
    }

    private object DECLARATION_ORIGIN_KPROPERTIES_FOR_DELEGATION :
            IrDeclarationOriginImpl("KPROPERTIES_FOR_DELEGATION")

    private fun createKPropertiesFieldDescriptor(containingDeclaration: DeclarationDescriptor, fieldType: SimpleType): PropertyDescriptorImpl {
        return PropertyDescriptorImpl.create(containingDeclaration, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE,
                false, "KPROPERTIES".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
                false, false, false, false, false, false).initialize(fieldType)
    }
}


/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor

import org.jetbrains.kotlin.backend.common.serialization.mangle.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.descriptors.IrImplementingDelegateDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrPropertyDelegateDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.types.typeUtil.isUnit

/**
 * The descriptor-based mangle computer. Used to compute a mangled name for a declaration given its [DeclarationDescriptor].
 */
open class DescriptorMangleComputer(builder: StringBuilder, mode: MangleMode) :
    BaseKotlinMangleComputer<
            /*Declaration=*/DeclarationDescriptor,
            /*Type=*/KotlinType,
            /*TypeParameter=*/TypeParameterDescriptor,
            /*ValueParameter=*/ValueParameterDescriptor,
            /*TypeParameterContainer=*/DeclarationDescriptor,
            /*FunctionDeclaration=*/FunctionDescriptor,
            /*Session=*/Nothing?,
            >(builder, mode) {
    final override fun getTypeSystemContext(session: Nothing?): TypeSystemContext = SimpleClassicTypeSystemContext

    override fun copy(newMode: MangleMode) = DescriptorMangleComputer(builder, newMode)

    final override fun DeclarationDescriptor.visitParent() {
        containingDeclaration?.visit()
    }

    final override fun DeclarationDescriptor.visit() {
        accept(Visitor(), null)
    }

    private fun reportUnexpectedDescriptor(descriptor: DeclarationDescriptor) {
        error("unexpected descriptor $descriptor")
    }

    open fun PropertyDescriptor.platformSpecificSuffix(): String? = null

    private val CallableDescriptor.isRealStatic: Boolean
        get() = dispatchReceiverParameter == null && containingDeclaration !is PackageFragmentDescriptor

    private fun FunctionDescriptor.mangleFunction(isCtor: Boolean, container: CallableDescriptor) {

        isRealExpect = isRealExpect or isExpect

        typeParameterContainers.add(container)
        container.containingDeclaration.visit()

        builder.appendName(MangleConstant.FUNCTION_NAME_PREFIX)

        platformSpecificFunctionName()?.let {
            builder.append(it)
            return
        }

        builder.append(name.asString())

        mangleSignature(isCtor, container)
    }

    private fun FunctionDescriptor.mangleSignature(isCtor: Boolean, realTypeParameterContainer: CallableDescriptor) {

        if (!mode.signature) return

        if (!isCtor && realTypeParameterContainer.isRealStatic) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        contextReceiverParameters.forEach {
            builder.appendSignature(MangleConstant.CONTEXT_RECEIVER_PREFIX)
            mangleContextReceiverParameter(builder, it)
        }

        extensionReceiverParameter?.let {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleExtensionReceiverParameter(builder, it)
        }

        valueParameters.collectForMangler(builder, MangleConstant.VALUE_PARAMETERS) {
            appendSignature(specialValueParamPrefix(it))
            mangleValueParameter(this, it, null)
        }
        realTypeParameterContainer.typeParameters.filter { it.containingDeclaration == realTypeParameterContainer }
            .collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it, it.index, null) }

        returnType?.run {
            if (!isCtor && !isUnit() && (addReturnType() || addReturnTypeSpecialCase(this@mangleSignature))) {
                mangleType(builder, this, null)
            }
        }

        platformSpecificSuffix()?.let {
            builder.appendSignature(MangleConstant.PLATFORM_FUNCTION_MARKER)
            builder.appendSignature(it)
        }
    }

    private fun mangleContextReceiverParameter(vpBuilder: StringBuilder, param: ReceiverParameterDescriptor) {
        mangleType(vpBuilder, param.type, null)
    }

    private fun mangleExtensionReceiverParameter(vpBuilder: StringBuilder, param: ReceiverParameterDescriptor) {
        mangleType(vpBuilder, param.type, null)
    }

    final override fun isVararg(valueParameter: ValueParameterDescriptor) = valueParameter.varargElementType != null

    final override fun getValueParameterType(valueParameter: ValueParameterDescriptor) = valueParameter.type

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    final override fun mangleType(tBuilder: StringBuilder, wrappedType: KotlinType, declarationSiteSession: Nothing?) {
        when (val type = wrappedType.unwrap()) {
            is SimpleType -> {

                if (type is SupposititiousSimpleType) {
                    val classId = type.overwrittenClass
                    classId.packageFqName.let {
                        if (!it.isRoot) {
                            builder.appendSignature(it.asString())
                            builder.appendSignature(MangleConstant.FQN_SEPARATOR)
                        }
                        builder.appendSignature(classId.relativeClassName.asString())
                    }
                } else {
                    when (val classifier = type.constructor.declarationDescriptor) {
                        is ClassDescriptor -> with(copy(MangleMode.FQNAME)) { classifier.visit() }
                        is TypeParameterDescriptor -> tBuilder.mangleTypeParameterReference(classifier)
                        else -> error("Unexpected classifier: $classifier")
                    }
                }

                mangleTypeArguments(tBuilder, type, null)

                if (type.isMarkedNullable) tBuilder.appendSignature(MangleConstant.Q_MARK)

                mangleTypePlatformSpecific(type, tBuilder)
            }
            is DynamicType -> tBuilder.appendSignature(MangleConstant.DYNAMIC_MARK)
            is FlexibleType -> {
                // Reproduce type approximation done for flexible types in TypeTranslator.
                val upper = type.upperBound
                val upperDescriptor = upper.constructor.declarationDescriptor
                    ?: error("No descriptor for type $upper")
                if (upperDescriptor is ClassDescriptor) {
                    val lower = type.lowerBound
                    val lowerDescriptor = lower.constructor.declarationDescriptor as? ClassDescriptor
                        ?: error("No class descriptor for lower type $lower of $type")
                    val intermediate = if (lowerDescriptor == upperDescriptor && type !is RawType) {
                        lower.replace(newArguments = upper.arguments)
                    } else lower
                    val mixed = intermediate.makeNullableAsSpecified(upper.isMarkedNullable)
                    mangleType(tBuilder, mixed, null)
                } else mangleType(tBuilder, upper, null)
            }
            else -> error("Unexpected type $wrappedType")
        }
    }

    final override fun getEffectiveParent(typeParameter: TypeParameterDescriptor) = typeParameter.containingDeclaration

    override fun renderDeclaration(declaration: DeclarationDescriptor) = DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(declaration)

    override fun getTypeParameterName(typeParameter: TypeParameterDescriptor) = typeParameter.name.asString()

    final override fun getIndexOfTypeParameter(typeParameter: TypeParameterDescriptor, container: DeclarationDescriptor) =
        typeParameter.index

    private fun manglePropertyAccessor(accessor: PropertyAccessorDescriptor) {
        val property = accessor.correspondingProperty
        accessor.mangleFunction(false, property)
    }

    protected open fun visitModuleDeclaration(descriptor: ModuleDescriptor) = reportUnexpectedDescriptor(descriptor)

    private inner class Visitor : DeclarationDescriptorVisitor<Unit, Nothing?> {

        override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Nothing?) {
            descriptor.fqName.let { if (!it.isRoot) builder.appendName(it.asString()) }
        }

        override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Nothing?) = reportUnexpectedDescriptor(descriptor)

        override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: Nothing?) = reportUnexpectedDescriptor(descriptor)

        override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Nothing?) {
            descriptor.mangleFunction(false, descriptor)
        }

        override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: Nothing?) {
            descriptor.containingDeclaration.visit()

            builder.appendSignature(MangleConstant.TYPE_PARAM_INDEX_PREFIX)
            builder.appendSignature(descriptor.index)
        }

        override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Nothing?) {
            isRealExpect = isRealExpect or descriptor.isExpect
            typeParameterContainers.add(descriptor)
            descriptor.mangleSimpleDeclaration(descriptor.name.asString())
        }

        override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Nothing?) {
            descriptor.mangleSimpleDeclaration(descriptor.name.asString())
        }

        override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Nothing?) {
            visitModuleDeclaration(descriptor)
        }

        override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: Nothing?) {
            constructorDescriptor.mangleFunction(isCtor = true, container = constructorDescriptor)
        }

        override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: Nothing?) =
            visitClassDescriptor(scriptDescriptor, data)

        override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Nothing?) {

            if (descriptor is IrImplementingDelegateDescriptor) {
                descriptor.mangleSimpleDeclaration(descriptor.name.asString())
            } else {

                val actualDescriptor = (descriptor as? IrPropertyDelegateDescriptor)?.correspondingProperty ?: descriptor

                val extensionReceiver = actualDescriptor.extensionReceiverParameter

                isRealExpect = isRealExpect or actualDescriptor.isExpect

                typeParameterContainers.add(actualDescriptor)
                actualDescriptor.containingDeclaration.visit()

                if (actualDescriptor.isRealStatic) {
                    builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
                }

                if (extensionReceiver != null) {
                    builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
                    mangleExtensionReceiverParameter(builder, extensionReceiver)
                }

                actualDescriptor.typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) {
                    mangleTypeParameter(this, it, it.index, null)
                }

                builder.append(actualDescriptor.name.asString())

                actualDescriptor.platformSpecificSuffix()?.let {
                    builder.appendSignature(it)
                }
            }
        }

        override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: Nothing?) =
            reportUnexpectedDescriptor(descriptor)

        override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: Nothing?) {
            manglePropertyAccessor(descriptor)
        }

        override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: Nothing?) {
            manglePropertyAccessor(descriptor)
        }

        override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: Nothing?) =
            reportUnexpectedDescriptor(descriptor)
    }
}

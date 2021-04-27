/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.collectForMangler
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.descriptors.IrImplementingDelegateDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrPropertyDelegateDescriptor
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class DescriptorMangleComputer(protected val builder: StringBuilder, private val mode: MangleMode, protected val typeApproximation: (KotlinType) -> KotlinType) :
    DeclarationDescriptorVisitor<Unit, (DeclarationDescriptor) -> String?>, KotlinMangleComputer<DeclarationDescriptor> {

    override fun computeMangle(declaration: DeclarationDescriptor, localNameResolver: (DeclarationDescriptor) -> String?): String {
        declaration.accept(this, localNameResolver)
        return builder.toString()
    }

    abstract override fun copy(newMode: MangleMode): DescriptorMangleComputer

    private fun StringBuilder.appendName(s: String) {
        if (mode.fqn) {
            append(s)
        }
    }

    private fun StringBuilder.appendName(c: Char) {
        if (mode.fqn) {
            append(c)
        }
    }

    private fun StringBuilder.appendSignature(s: String) {
        if (mode.signature) {
            append(s)
        }
    }

    private fun StringBuilder.appendSignature(c: Char) {
        if (mode.signature) {
            append(c)
        }
    }

    private fun StringBuilder.appendSignature(i: Int) {
        if (mode.signature) {
            append(i)
        }
    }

    private val typeParameterContainer = ArrayList<DeclarationDescriptor>(4)

    private var isRealExpect = false

    private fun DeclarationDescriptor.mangleSimpleDeclaration(name: String, localNameResolver: (DeclarationDescriptor) -> String?) {
        val l = builder.length
        containingDeclaration?.accept(this@DescriptorMangleComputer, localNameResolver)

        if (builder.length != l) builder.appendName(MangleConstant.FQN_SEPARATOR)

        builder.appendName(name)
    }

    open fun FunctionDescriptor.platformSpecificFunctionName(): String? = null

    private fun reportUnexpectedDescriptor(descriptor: DeclarationDescriptor) {
        println("unexpected descriptor $descriptor")
    }

    open fun FunctionDescriptor.platformSpecificSuffix(): String? = null
    open fun PropertyDescriptor.platformSpecificSuffix(): String? = null

    protected open fun addReturnType(): Boolean = false

    protected open fun addReturnTypeSpecialCase(functionDescriptor: FunctionDescriptor): Boolean = false

    open fun FunctionDescriptor.specialValueParamPrefix(param: ValueParameterDescriptor): String = ""

    private val CallableDescriptor.isRealStatic: Boolean
        get() = dispatchReceiverParameter == null && containingDeclaration !is PackageFragmentDescriptor

    private fun FunctionDescriptor.mangleFunction(isCtor: Boolean, container: CallableDescriptor, localNameResolver: (DeclarationDescriptor) -> String?) {

        isRealExpect = isRealExpect or isExpect

        typeParameterContainer.add(container)
        container.containingDeclaration.accept(this@DescriptorMangleComputer, localNameResolver)

        builder.appendName(MangleConstant.FUNCTION_NAME_PREFIX)

        platformSpecificFunctionName()?.let {
            builder.append(it)
            return
        }


        val funName = (if (visibility == DescriptorVisibilities.LOCAL) localNameResolver(this) else null) ?: name.asString()

        builder.append(funName)

        mangleSignature(isCtor, container, localNameResolver)
    }

    private fun FunctionDescriptor.mangleSignature(isCtor: Boolean, realTypeParameterContainer: CallableDescriptor, localNameResolver: (DeclarationDescriptor) -> String?) {

        if (!mode.signature) return

        if (!isCtor && realTypeParameterContainer.isRealStatic) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        extensionReceiverParameter?.let {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleExtensionReceiverParameter(builder, it, localNameResolver)
        }

        valueParameters.collectForMangler(builder, MangleConstant.VALUE_PARAMETERS) {
            appendSignature(specialValueParamPrefix(it))
            mangleValueParameter(this, it, localNameResolver)
        }
        realTypeParameterContainer.typeParameters.filter { it.containingDeclaration == realTypeParameterContainer }
            .collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it, localNameResolver) }

        returnType?.run {
            if (!isCtor && !isUnit() && (addReturnType() || addReturnTypeSpecialCase(this@mangleSignature))) {
                mangleType(builder, this, localNameResolver)
            }
        }

        platformSpecificSuffix()?.let {
            builder.appendSignature(MangleConstant.PLATFORM_FUNCTION_MARKER)
            builder.appendSignature(it)
        }
    }

    private fun mangleExtensionReceiverParameter(vpBuilder: StringBuilder, param: ReceiverParameterDescriptor, resolveLocalName: (DeclarationDescriptor) -> String?) {
        mangleType(vpBuilder, param.type, resolveLocalName)
    }

    private fun mangleValueParameter(vpBuilder: StringBuilder, param: ValueParameterDescriptor, resolveLocalName: (DeclarationDescriptor) -> String?) {
        mangleType(vpBuilder, param.type, resolveLocalName)

        if (param.varargElementType != null) vpBuilder.appendSignature(MangleConstant.VAR_ARG_MARK)
    }

    private fun mangleTypeParameter(tpBuilder: StringBuilder, param: TypeParameterDescriptor, resolveLocalName: (DeclarationDescriptor) -> String?) {
        tpBuilder.appendSignature(param.index)
        tpBuilder.appendSignature(MangleConstant.UPPER_BOUND_SEPARATOR)

        param.upperBounds.collectForMangler(tpBuilder, MangleConstant.UPPER_BOUNDS) { mangleType(this, it, resolveLocalName) }
    }

    private fun mangleType(tBuilder: StringBuilder, wtype: KotlinType, localNameResolver: (DeclarationDescriptor) -> String?) {
        when (val utype = wtype.unwrap()) {
            is SimpleType -> {

                val type = typeApproximation(utype)

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
                        is ClassDescriptor -> classifier.accept(copy(MangleMode.FQNAME), localNameResolver)
                        is TypeParameterDescriptor -> tBuilder.mangleTypeParameterReference(classifier)
                        else -> error("Unexpected classifier: $classifier")
                    }
                }

                type.arguments.ifNotEmpty {
                    collectForMangler(tBuilder, MangleConstant.TYPE_ARGUMENTS) { arg ->
                        if (arg.isStarProjection) {
                            appendSignature(MangleConstant.STAR_MARK)
                        } else {
                            if (arg.projectionKind != Variance.INVARIANT) {
                                appendSignature(arg.projectionKind.label)
                                appendSignature(MangleConstant.VARIANCE_SEPARATOR)
                            }

                            mangleType(this, arg.type, localNameResolver)
                        }
                    }
                }

                if (type.isMarkedNullable) tBuilder.appendSignature(MangleConstant.Q_MARK)

                // Disambiguate between 'double' and '@NotNull java.lang.Double' types in mixed Java/Kotlin class hierarchies
                if (SimpleClassicTypeSystemContext.hasEnhancedNullability(type)) {
                    tBuilder.appendSignature(MangleConstant.ENHANCED_NULLABILITY_MARK)
                }
            }
            is DynamicType -> tBuilder.appendSignature(MangleConstant.DYNAMIC_MARK)
            is FlexibleType -> {
                // TODO: is that correct way to mangle flexible type?
                with(MangleConstant.FLEXIBLE_TYPE) {
                    tBuilder.appendSignature(prefix)
                    mangleType(tBuilder, utype.lowerBound, localNameResolver)
                    tBuilder.appendSignature(separator)
                    mangleType(tBuilder, utype.upperBound, localNameResolver)
                    tBuilder.appendSignature(suffix)
                }
            }
            else -> error("Unexpected type $wtype")
        }
    }

    private fun StringBuilder.mangleTypeParameterReference(typeParameter: TypeParameterDescriptor) {
        val parent = typeParameter.containingDeclaration
        val ci = typeParameterContainer.indexOf(parent)
        // TODO: what should we do in this case?
//            require(ci >= 0) { "No type container found for ${typeParameter.render()}" }
        appendSignature(ci)
        appendSignature(MangleConstant.INDEX_SEPARATOR)
        appendSignature(typeParameter.index)
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: (DeclarationDescriptor) -> String?) {
        descriptor.fqName.let { if (!it.isRoot) builder.appendName(it.asString()) }
    }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: (DeclarationDescriptor) -> String?) = reportUnexpectedDescriptor(descriptor)

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: (DeclarationDescriptor) -> String?) = reportUnexpectedDescriptor(descriptor)

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: (DeclarationDescriptor) -> String?) {
        descriptor.mangleFunction(false, descriptor, data)
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: (DeclarationDescriptor) -> String?) {
        descriptor.containingDeclaration.accept(this, data)

        builder.appendSignature(MangleConstant.TYPE_PARAM_INDEX_PREFIX)
        builder.appendSignature(descriptor.index)
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: (DeclarationDescriptor) -> String?) {
        isRealExpect = isRealExpect or descriptor.isExpect
        typeParameterContainer.add(descriptor)
        val className = (if (descriptor.visibility == DescriptorVisibilities.LOCAL) data(descriptor) else null) ?: descriptor.name.asString()
//        val className = data(descriptor) ?: descriptor.name.asString()
        descriptor.mangleSimpleDeclaration(className, data)
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: (DeclarationDescriptor) -> String?) {
        descriptor.mangleSimpleDeclaration(descriptor.name.asString(), data)
    }

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: (DeclarationDescriptor) -> String?) = reportUnexpectedDescriptor(descriptor)

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: (DeclarationDescriptor) -> String?) {
        constructorDescriptor.mangleFunction(isCtor = true, container = constructorDescriptor, data)
    }

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: (DeclarationDescriptor) -> String?) = reportUnexpectedDescriptor(scriptDescriptor)

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: (DeclarationDescriptor) -> String?) {

        if (descriptor is IrImplementingDelegateDescriptor) {
            descriptor.mangleSimpleDeclaration(descriptor.name.asString(), data)
//            mangleType(builder, descriptor.type)
        } else {

            val actualDescriptor = (descriptor as? IrPropertyDelegateDescriptor)?.correspondingProperty ?: descriptor

            val extensionReceiver = actualDescriptor.extensionReceiverParameter

            isRealExpect = isRealExpect or actualDescriptor.isExpect

            typeParameterContainer.add(actualDescriptor)
            actualDescriptor.containingDeclaration.accept(this, data)

            if (actualDescriptor.isRealStatic) {
                builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
            }

            if (extensionReceiver != null) {
                builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
                mangleExtensionReceiverParameter(builder, extensionReceiver, data)
            }

            actualDescriptor.typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it, data) }

            builder.append(actualDescriptor.name.asString())

            actualDescriptor.platformSpecificSuffix()?.let {
                builder.appendSignature(it)
            }
        }
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: (DeclarationDescriptor) -> String?) = reportUnexpectedDescriptor(descriptor)

    private fun manglePropertyAccessor(accessor: PropertyAccessorDescriptor, resolveLocalName: (DeclarationDescriptor) -> String?) {
        val property = accessor.correspondingProperty
        accessor.mangleFunction(false, property, resolveLocalName)
    }

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: (DeclarationDescriptor) -> String?) {
        manglePropertyAccessor(descriptor, data)
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: (DeclarationDescriptor) -> String?) {
        manglePropertyAccessor(descriptor, data)
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: (DeclarationDescriptor) -> String?) =
        reportUnexpectedDescriptor(descriptor)
}
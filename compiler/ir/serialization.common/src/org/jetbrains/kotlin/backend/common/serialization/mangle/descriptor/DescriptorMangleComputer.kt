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
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class DescriptorMangleComputer(protected val builder: StringBuilder, private val mode: MangleMode) :
    DeclarationDescriptorVisitor<Unit, Boolean>, KotlinMangleComputer<DeclarationDescriptor> {

    override fun computeMangle(declaration: DeclarationDescriptor): String {
        declaration.accept(this, true)
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

    private fun DeclarationDescriptor.mangleSimpleDeclaration(name: String) {
        val l = builder.length
        containingDeclaration?.accept(this@DescriptorMangleComputer, false)

        if (builder.length != l) builder.appendName(MangleConstant.FQN_SEPARATOR)

        builder.appendName(name)
    }

    open fun FunctionDescriptor.platformSpecificFunctionName(): String? = null

    private fun reportUnexpectedDescriptor(descriptor: DeclarationDescriptor) {
        error("unexpected descriptor $descriptor")
    }

    open fun FunctionDescriptor.platformSpecificSuffix(): String? = null
    open fun PropertyDescriptor.platformSpecificSuffix(): String? = null

    protected open fun addReturnType(): Boolean = false

    open fun FunctionDescriptor.specialValueParamPrefix(param: ValueParameterDescriptor): String = ""

    private val CallableDescriptor.isRealStatic: Boolean
        get() = dispatchReceiverParameter == null && containingDeclaration !is PackageFragmentDescriptor

    private fun FunctionDescriptor.mangleFunction(isCtor: Boolean, container: CallableDescriptor) {

        isRealExpect = isRealExpect or isExpect

        typeParameterContainer.add(container)
        container.containingDeclaration.accept(this@DescriptorMangleComputer, false)

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

        extensionReceiverParameter?.let {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleExtensionReceiverParameter(builder, it)
        }

        valueParameters.collectForMangler(builder, MangleConstant.VALUE_PARAMETERS) {
            appendSignature(specialValueParamPrefix(it))
            mangleValueParameter(this, it)
        }
        realTypeParameterContainer.typeParameters.filter { it.containingDeclaration == realTypeParameterContainer }
            .collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it) }

        returnType?.run {
            if (!isCtor && !isUnit() && addReturnType()) {
                mangleType(builder, this)
            }
        }

        platformSpecificSuffix()?.let {
            builder.appendSignature(MangleConstant.PLATFORM_FUNCTION_MARKER)
            builder.appendSignature(it)
        }
    }

    private fun mangleExtensionReceiverParameter(vpBuilder: StringBuilder, param: ReceiverParameterDescriptor) {
        mangleType(vpBuilder, param.type)
    }

    private fun mangleValueParameter(vpBuilder: StringBuilder, param: ValueParameterDescriptor) {
        mangleType(vpBuilder, param.type)

        if (param.varargElementType != null) vpBuilder.appendSignature(MangleConstant.VAR_ARG_MARK)
    }

    private fun mangleTypeParameter(tpBuilder: StringBuilder, param: TypeParameterDescriptor) {
        tpBuilder.appendSignature(param.index)
        tpBuilder.appendSignature(MangleConstant.UPPER_BOUND_SEPARATOR)

        param.upperBounds.collectForMangler(tpBuilder, MangleConstant.UPPER_BOUNDS) { mangleType(this, it) }
    }

    private fun mangleType(tBuilder: StringBuilder, wtype: KotlinType) {
        when (val type = wtype.unwrap()) {
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
                        is ClassDescriptor -> classifier.accept(copy(MangleMode.FQNAME), false)
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

                            mangleType(this, arg.type)
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
                    mangleType(tBuilder, type.lowerBound)
                    tBuilder.appendSignature(separator)
                    mangleType(tBuilder, type.upperBound)
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

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Boolean) {
        descriptor.fqName.let { if (!it.isRoot) builder.appendName(it.asString()) }
    }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Boolean) = reportUnexpectedDescriptor(descriptor)

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: Boolean) = reportUnexpectedDescriptor(descriptor)

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Boolean) {
        descriptor.mangleFunction(false, descriptor)
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: Boolean) {
        descriptor.containingDeclaration.accept(this, data)

        builder.appendSignature(MangleConstant.TYPE_PARAM_INDEX_PREFIX)
        builder.appendSignature(descriptor.index)
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Boolean) {
        isRealExpect = isRealExpect or descriptor.isExpect
        typeParameterContainer.add(descriptor)
        descriptor.mangleSimpleDeclaration(descriptor.name.asString())
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Boolean) {
        descriptor.mangleSimpleDeclaration(descriptor.name.asString())
    }

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Boolean) = reportUnexpectedDescriptor(descriptor)

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: Boolean) {
        constructorDescriptor.mangleFunction(isCtor = true, container = constructorDescriptor)
    }

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: Boolean) = reportUnexpectedDescriptor(scriptDescriptor)

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Boolean) {
        val extensionReceiver = descriptor.extensionReceiverParameter

        isRealExpect = isRealExpect or descriptor.isExpect

        typeParameterContainer.add(descriptor)
        descriptor.containingDeclaration.accept(this, false)

        if (descriptor.isRealStatic) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        if (extensionReceiver != null) {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleExtensionReceiverParameter(builder, extensionReceiver)
        }

        descriptor.typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it) }

        builder.append(descriptor.name.asString())

        descriptor.platformSpecificSuffix()?.let {
            builder.appendSignature(it)
        }
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: Boolean) = reportUnexpectedDescriptor(descriptor)

    private fun manglePropertyAccessor(accessor: PropertyAccessorDescriptor) {
        val property = accessor.correspondingProperty
        accessor.mangleFunction(false, property)
    }

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: Boolean) {
        manglePropertyAccessor(descriptor)
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: Boolean) {
        manglePropertyAccessor(descriptor)
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: Boolean) =
        reportUnexpectedDescriptor(descriptor)
}
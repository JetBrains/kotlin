/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.collect
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class DescriptorMangleComputer(protected val builder: StringBuilder, protected val specialPrefix: String) :
    DeclarationDescriptorVisitor<Unit, Boolean>, KotlinMangleComputer<DeclarationDescriptor> {

    override fun computeMangle(declaration: DeclarationDescriptor): String {
        declaration.accept(this, true)
        return builder.toString()
    }

    protected abstract fun copy(): DescriptorMangleComputer

    private val typeParameterContainer = ArrayList<DeclarationDescriptor>(4)

    private var isRealExpect = false

    private fun addPrefix(prefix: String, addPrefix: Boolean): Int {
        if (addPrefix) {
            builder.append(prefix)
            builder.append(MangleConstant.PREFIX_SEPARATOR)
        }
        return builder.length
    }

    private fun DeclarationDescriptor.mangleSimpleDeclaration(prefix: String, addPrefix: Boolean, name: String) {
        val prefixLength = addPrefix(prefix, addPrefix)
        containingDeclaration?.accept(this@DescriptorMangleComputer, false)

        if (prefixLength != builder.length) builder.append(MangleConstant.FQN_SEPARATOR)

        builder.append(name)
    }

    open val FunctionDescriptor.platformSpecificFunctionName: String? get() = null

    private fun reportUnexpectedDescriptor(descriptor: DeclarationDescriptor) {
        error("unexpected descriptor $descriptor")
    }

    open fun FunctionDescriptor.platformSpecificSuffix(): String? = null

    private fun FunctionDescriptor.mangleFunction(isCtor: Boolean, prefix: Boolean, container: CallableDescriptor) {

        isRealExpect = isRealExpect or isExpect

        val prefixLength = addPrefix(MangleConstant.FUN_PREFIX, prefix)

        typeParameterContainer.add(container)
        container.containingDeclaration.accept(this@DescriptorMangleComputer, false)

        if (prefixLength != builder.length) builder.append(MangleConstant.FQN_SEPARATOR)

        builder.append(MangleConstant.FUNCTION_NAME_PREFIX)

        if (visibility != Visibilities.INTERNAL) builder.append(name)
        else {
            builder.append(name)
            builder.append(MangleConstant.MODULE_SEPARATOR)
            builder.append(module.name.asString().run { substring(1, lastIndex) })
        }

        mangleSignature(isCtor, container)

        platformSpecificSuffix()?.let {
            builder.append(MangleConstant.PLATFORM_FUNCTION_MARKER)
            builder.append(it)
        }

        if (prefix && isRealExpect) builder.append(MangleConstant.EXPECT_MARK)
    }

    private fun FunctionDescriptor.mangleSignature(isCtor: Boolean, realTypeParameterContainer: CallableDescriptor) {

        extensionReceiverParameter?.let {
            builder.append(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleExtensionReceiverParameter(builder, it)
        }

        valueParameters.collect(builder, MangleConstant.VALUE_PARAMETERS) { mangleValueParameter(this, it) }
        realTypeParameterContainer.typeParameters.filter { it.containingDeclaration == realTypeParameterContainer }
            .collect(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it) }

        returnType?.run {
            if (!isCtor && !isUnit()) {
                mangleType(builder, this)
            }
        }
    }

    private fun mangleExtensionReceiverParameter(vpBuilder: StringBuilder, param: ReceiverParameterDescriptor) {
        mangleType(vpBuilder, param.type)
    }

    private fun mangleValueParameter(vpBuilder: StringBuilder, param: ValueParameterDescriptor) {
        mangleType(vpBuilder, param.type)

        if (param.varargElementType != null) vpBuilder.append(MangleConstant.VAR_ARG_MARK)
    }

    private fun mangleTypeParameter(tpBuilder: StringBuilder, param: TypeParameterDescriptor) {
        tpBuilder.append(param.index)
        tpBuilder.append(MangleConstant.UPPER_BOUND_SEPARATOR)

        param.upperBounds.collect(tpBuilder, MangleConstant.UPPER_BOUNDS) { mangleType(this, it) }
    }

    private fun mangleType(tBuilder: StringBuilder, wtype: KotlinType) {
        when (val type = wtype.unwrap()) {
            is SimpleType -> {
                when (val classifier = type.constructor.declarationDescriptor) {
                    is ClassDescriptor -> classifier.accept(copy(), false)
                    is TypeParameterDescriptor -> tBuilder.mangleTypeParameterReference(classifier)
                    else -> error("Unexpected classifier: $classifier")
                }

                type.arguments.ifNotEmpty {
                    collect(tBuilder, MangleConstant.TYPE_ARGUMENTS) { arg ->
                        if (arg.isStarProjection) {
                            append(MangleConstant.STAR_MARK)
                        } else {
                            if (arg.projectionKind != Variance.INVARIANT) {
                                append(arg.projectionKind.label)
                                append(MangleConstant.VARIANCE_SEPARATOR)
                            }

                            mangleType(this, arg.type)
                        }
                    }
                }

                if (type.isMarkedNullable) tBuilder.append(MangleConstant.Q_MARK)
            }
            is DynamicType -> tBuilder.append(MangleConstant.DYNAMIC_MARK)
            is FlexibleType -> {
                // TODO: is that correct way to mangle flexible type?
                with(MangleConstant.FLEXIBLE_TYPE) {
                    tBuilder.append(prefix)
                    mangleType(tBuilder, type.lowerBound)
                    tBuilder.append(separator)
                    mangleType(tBuilder, type.upperBound)
                    tBuilder.append(suffix)
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
        append(ci)
        append(MangleConstant.INDEX_SEPARATOR)
        append(typeParameter.index)
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: Boolean) {
        descriptor.fqName.let { if (!it.isRoot) builder.append(it.asString()) }
    }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: Boolean) = reportUnexpectedDescriptor(descriptor)

    override fun visitVariableDescriptor(descriptor: VariableDescriptor, data: Boolean) = reportUnexpectedDescriptor(descriptor)

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Boolean) {
        descriptor.platformSpecificFunctionName?.let {
            builder.append(it)
            return
        }

        descriptor.mangleFunction(false, data, descriptor)
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: Boolean) {
        addPrefix(MangleConstant.TYPE_PARAM_PREFIX, data)
        descriptor.containingDeclaration.accept(this, data)

        builder.append(MangleConstant.TYPE_PARAM_INDEX_PREFIX)
        builder.append(descriptor.index)
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: Boolean) {
        isRealExpect = isRealExpect or descriptor.isExpect
        typeParameterContainer.add(descriptor)
        val prefix = if (specialPrefix == MangleConstant.ENUM_ENTRY_PREFIX) specialPrefix else MangleConstant.CLASS_PREFIX
        descriptor.mangleSimpleDeclaration(prefix, data, descriptor.name.asString())

        if (data && isRealExpect) builder.append(MangleConstant.EXPECT_MARK)
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: Boolean) {
        descriptor.mangleSimpleDeclaration(MangleConstant.TYPE_ALIAS_PREFIX, data, descriptor.name.asString())
    }

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Boolean) = reportUnexpectedDescriptor(descriptor)

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: Boolean) {
        constructorDescriptor.mangleFunction(true, data, constructorDescriptor)
    }

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: Boolean) = reportUnexpectedDescriptor(scriptDescriptor)

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Boolean) {
        val extensionReceiver = descriptor.extensionReceiverParameter

        val prefix = if (specialPrefix == MangleConstant.FIELD_PREFIX) specialPrefix else MangleConstant.PROPERTY_PREFIX
        val prefixLength = addPrefix(prefix, data)

        isRealExpect = isRealExpect or descriptor.isExpect
        typeParameterContainer.add(descriptor)
        descriptor.containingDeclaration.accept(this, false)

        if (prefixLength != builder.length) builder.append(MangleConstant.FQN_SEPARATOR)

        if (extensionReceiver != null) {
            builder.append(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleExtensionReceiverParameter(builder, extensionReceiver)
        }

        builder.append(descriptor.name)
        if (data && isRealExpect) builder.append(MangleConstant.EXPECT_MARK)
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: Boolean) = reportUnexpectedDescriptor(descriptor)

    private fun manglePropertyAccessor(accessor: PropertyAccessorDescriptor, data: Boolean) {
        accessor.mangleFunction(false, data, accessor.correspondingProperty)
    }

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: Boolean) {
        manglePropertyAccessor(descriptor, data)
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: Boolean) {
        manglePropertyAccessor(descriptor, data)
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: Boolean) =
        reportUnexpectedDescriptor(descriptor)
}
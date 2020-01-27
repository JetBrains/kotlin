/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.ir

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.collect
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class IrMangleComputer(protected val builder: StringBuilder) : IrElementVisitor<Unit, Boolean>,
    KotlinMangleComputer<IrDeclaration> {

    private val typeParameterContainer = ArrayList<IrDeclaration>(4)

    private var isRealExpect = false

    open val IrFunction.platformSpecificFunctionName: String? get() = null

    protected abstract fun copy(): IrMangleComputer

    override fun computeMangle(declaration: IrDeclaration): String {
        declaration.accept(this, true)
        return builder.toString()
    }

    private fun addPrefix(prefix: String, addPrefix: Boolean): Int {
        if (addPrefix) {
            builder.append(prefix)
            builder.append(MangleConstant.PREFIX_SEPARATOR)
        }
        return builder.length
    }

    private fun IrDeclaration.mangleSimpleDeclaration(prefix: String, addPrefix: Boolean, name: String) {
        val prefixLength = addPrefix(prefix, addPrefix)
        parent.accept(this@IrMangleComputer, false)

        if (prefixLength != builder.length) builder.append(MangleConstant.FQN_SEPARATOR)

        builder.append(name)
    }

    private fun IrFunction.mangleFunction(isCtor: Boolean, prefix: Boolean, container: IrDeclaration) {

        isRealExpect = isRealExpect or isExpect

        val prefixLength = addPrefix(MangleConstant.FUN_PREFIX, prefix)

        typeParameterContainer.add(container)
        container.parent.accept(this@IrMangleComputer, false)

        if (prefixLength != builder.length) builder.append(MangleConstant.FQN_SEPARATOR)

        builder.append(MangleConstant.FUNCTION_NAME_PREFIX)

        if (visibility != Visibilities.INTERNAL) builder.append(name)
        else {
            builder.append(name)
            builder.append(MangleConstant.MODULE_SEPARATOR)
            val moduleName = try {
                module.name.asString().run { substring(1, lastIndex) }
            } catch (e: Throwable) {
                MangleConstant.UNKNOWN_MARK
            }
            builder.append(moduleName)
        }

        mangleSignature(isCtor)

        if (prefix && isRealExpect) builder.append(MangleConstant.EXPECT_MARK)
    }

    private fun IrFunction.mangleSignature(isCtor: Boolean) {

        extensionReceiverParameter?.let {
            builder.append(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleValueParameter(builder, it)
        }

        valueParameters.collect(builder, MangleConstant.VALUE_PARAMETERS) { mangleValueParameter(this, it) }
        typeParameters.collect(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it) }

        if (!isCtor && !returnType.isUnit()) {
            mangleType(builder, returnType)
        }
    }

    private fun IrTypeParameter.effectiveParent(): IrDeclaration = when (val irParent = parent) {
        is IrSimpleFunction -> irParent.correspondingPropertySymbol?.owner ?: irParent
        is IrTypeParametersContainer -> irParent
        else -> error("Unexpected type parameter container ${irParent.render()} for TP ${render()}")
    }

    private fun mangleValueParameter(vpBuilder: StringBuilder, param: IrValueParameter) {
        mangleType(vpBuilder, param.type)

        if (param.isVararg) vpBuilder.append(MangleConstant.VAR_ARG_MARK)
    }

    private fun mangleTypeParameter(tpBuilder: StringBuilder, param: IrTypeParameter) {
        tpBuilder.append(param.index)
        tpBuilder.append(MangleConstant.UPPER_BOUND_SEPARATOR)

        param.superTypes.collect(tpBuilder, MangleConstant.UPPER_BOUNDS) { mangleType(this, it) }
    }

    private fun StringBuilder.mangleTypeParameterReference(typeParameter: IrTypeParameter) {
        val parent = typeParameter.effectiveParent()
        val ci = typeParameterContainer.indexOf(parent)
        // TODO: what should we do in this case?
//            require(ci >= 0) { "No type container found for ${typeParameter.render()}" }
        append(ci)
        append(MangleConstant.INDEX_SEPARATOR)
        append(typeParameter.index)
    }

    private fun mangleType(tBuilder: StringBuilder, type: IrType) {
        when (type) {
            is IrSimpleType -> {
                when (val classifier = type.classifier) {
                    is IrClassSymbol -> classifier.owner.accept(copy(), false)
                    is IrTypeParameterSymbol -> tBuilder.mangleTypeParameterReference(classifier.owner)
                }

                type.arguments.ifNotEmpty {
                    collect(tBuilder, MangleConstant.TYPE_ARGUMENTS) { arg ->
                        when (arg) {
                            is IrStarProjection -> append(MangleConstant.STAR_MARK)
                            is IrTypeProjection -> {
                                if (arg.variance != Variance.INVARIANT) {
                                    append(arg.variance.label)
                                    append(MangleConstant.VARIANCE_SEPARATOR)
                                }

                                mangleType(this, arg.type)
                            }
                        }
                    }
                }

                if (type.hasQuestionMark) tBuilder.append(MangleConstant.Q_MARK)
            }
            is IrDynamicType -> tBuilder.append(MangleConstant.DYNAMIC_MARK)
            else -> error("Unexpected type $type")
        }
    }

    override fun visitElement(element: IrElement, data: Boolean) = error("unexpected element ${element.render()}")

    override fun visitClass(declaration: IrClass, data: Boolean) {
        isRealExpect = isRealExpect or declaration.isExpect
        typeParameterContainer.add(declaration)
        declaration.mangleSimpleDeclaration(MangleConstant.CLASS_PREFIX, data, declaration.name.asString())

        if (data && isRealExpect) builder.append(MangleConstant.EXPECT_MARK)
    }

    override fun visitPackageFragment(declaration: IrPackageFragment, data: Boolean) {
        declaration.fqName.let { if (!it.isRoot) builder.append(it.asString()) }
    }

    override fun visitProperty(declaration: IrProperty, data: Boolean) {
        val extensionReceiver = declaration.run { (getter ?: setter)?.extensionReceiverParameter }

        val prefixLength = addPrefix(MangleConstant.PROPERTY_PREFIX, data)

        isRealExpect = isRealExpect or declaration.isExpect
        typeParameterContainer.add(declaration)
        declaration.parent.accept(this, false)

        if (prefixLength != builder.length) builder.append(MangleConstant.FQN_SEPARATOR)

        if (extensionReceiver != null) {
            builder.append(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleValueParameter(builder, extensionReceiver)
        }

        builder.append(declaration.name)
        if (data && isRealExpect) builder.append(MangleConstant.EXPECT_MARK)
    }

    override fun visitField(declaration: IrField, data: Boolean) =
        declaration.mangleSimpleDeclaration(MangleConstant.FIELD_PREFIX, data, declaration.name.asString())

    override fun visitEnumEntry(declaration: IrEnumEntry, data: Boolean) {
        declaration.mangleSimpleDeclaration(MangleConstant.ENUM_ENTRY_PREFIX, data, declaration.name.asString())
        if (data && isRealExpect) builder.append(MangleConstant.EXPECT_MARK)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Boolean) =
        declaration.mangleSimpleDeclaration(MangleConstant.TYPE_ALIAS_PREFIX, data, declaration.name.asString())

    override fun visitTypeParameter(declaration: IrTypeParameter, data: Boolean) {
        addPrefix(MangleConstant.TYPE_PARAM_PREFIX, data)
        declaration.effectiveParent().accept(this, data)

        builder.append(MangleConstant.TYPE_PARAM_INDEX_PREFIX)
        builder.append(declaration.index)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Boolean) {

        isRealExpect = isRealExpect or declaration.isExpect

        declaration.platformSpecificFunctionName?.let {
            builder.append(it)
            return
        }

        val container = declaration.correspondingPropertySymbol?.owner ?: declaration

        declaration.mangleFunction(false, data, container)
    }

    override fun visitConstructor(declaration: IrConstructor, data: Boolean) = declaration.mangleFunction(true, data, declaration)
}

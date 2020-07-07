/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.ir

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.collectForMangler
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class IrMangleComputer(protected val builder: StringBuilder, private val mode: MangleMode) :
    IrElementVisitor<Unit, Boolean>, KotlinMangleComputer<IrDeclaration> {

    private val typeParameterContainer = ArrayList<IrDeclaration>(4)

    private var isRealExpect = false

    open fun IrFunction.platformSpecificFunctionName(): String? = null

    open fun IrFunction.specialValueParamPrefix(param: IrValueParameter): String = ""

    open fun addReturnType(): Boolean = false

    abstract override fun copy(newMode: MangleMode): IrMangleComputer

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

    override fun computeMangle(declaration: IrDeclaration): String {
        declaration.accept(this, true)
        return builder.toString()
    }

    private fun IrDeclaration.mangleSimpleDeclaration(name: String) {
        val l = builder.length
        parent.accept(this@IrMangleComputer, false)

        if (builder.length != l) builder.appendName(MangleConstant.FQN_SEPARATOR)

        builder.appendName(name)
    }

    private fun IrFunction.mangleFunction(isCtor: Boolean, isStatic: Boolean, container: IrDeclaration) {

        isRealExpect = isRealExpect or isExpect

        typeParameterContainer.add(container)
        container.parent.accept(this@IrMangleComputer, false)

        builder.appendName(MangleConstant.FUNCTION_NAME_PREFIX)

        platformSpecificFunctionName()?.let {
            builder.append(it)
            return
        }

        builder.append(name.asString())

        mangleSignature(isCtor, isStatic)
    }

    private fun IrFunction.mangleSignature(isCtor: Boolean, isStatic: Boolean) {
        if (!mode.signature) return

        if (isStatic) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        extensionReceiverParameter?.let {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleValueParameter(builder, it)
        }

        valueParameters.collectForMangler(builder, MangleConstant.VALUE_PARAMETERS) {
            appendSignature(specialValueParamPrefix(it))
            mangleValueParameter(this, it)
        }
        typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it) }

        if (!isCtor && !returnType.isUnit() && addReturnType()) {
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

        if (param.isVararg) vpBuilder.appendSignature(MangleConstant.VAR_ARG_MARK)
    }

    private fun mangleTypeParameter(tpBuilder: StringBuilder, param: IrTypeParameter) {
        tpBuilder.appendSignature(param.index)
        tpBuilder.appendSignature(MangleConstant.UPPER_BOUND_SEPARATOR)

        param.superTypes.collectForMangler(tpBuilder, MangleConstant.UPPER_BOUNDS) { mangleType(this, it) }
    }

    private fun StringBuilder.mangleTypeParameterReference(typeParameter: IrTypeParameter) {
        val parent = typeParameter.effectiveParent()
        val ci = typeParameterContainer.indexOf(parent)
        // TODO: what should we do in this case?
//            require(ci >= 0) { "No type container found for ${typeParameter.render()}" }
        appendSignature(ci)
        appendSignature(MangleConstant.INDEX_SEPARATOR)
        appendSignature(typeParameter.index)
    }

    private fun mangleType(tBuilder: StringBuilder, type: IrType) {
        when (type) {
            is IrSimpleType -> {
                when (val classifier = type.classifier) {
                    is IrClassSymbol -> classifier.owner.accept(copy(MangleMode.FQNAME), false)
                    is IrTypeParameterSymbol -> tBuilder.mangleTypeParameterReference(classifier.owner)
                }

                type.arguments.ifNotEmpty {
                    collectForMangler(tBuilder, MangleConstant.TYPE_ARGUMENTS) { arg ->
                        when (arg) {
                            is IrStarProjection -> appendSignature(MangleConstant.STAR_MARK)
                            is IrTypeProjection -> {
                                if (arg.variance != Variance.INVARIANT) {
                                    appendSignature(arg.variance.label)
                                    appendSignature(MangleConstant.VARIANCE_SEPARATOR)
                                }

                                mangleType(this, arg.type)
                            }
                        }
                    }
                }

                if (type.hasQuestionMark) tBuilder.appendSignature(MangleConstant.Q_MARK)
            }
            is IrDynamicType -> tBuilder.appendSignature(MangleConstant.DYNAMIC_MARK)
            is IrErrorType -> tBuilder.appendSignature(MangleConstant.ERROR_MARK)
            else -> error("Unexpected type $type")
        }
    }

    override fun visitElement(element: IrElement, data: Boolean) = error("unexpected element ${element.render()}")

    override fun visitScript(declaration: IrScript, data: Boolean) {
        declaration.parent.accept(this, data)
    }

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Boolean) {
        declaration.mangleSimpleDeclaration(MangleConstant.ERROR_DECLARATION)
    }

    override fun visitClass(declaration: IrClass, data: Boolean) {
        isRealExpect = isRealExpect or declaration.isExpect
        typeParameterContainer.add(declaration)
        declaration.mangleSimpleDeclaration(declaration.name.asString())
    }

    override fun visitPackageFragment(declaration: IrPackageFragment, data: Boolean) {
        declaration.fqName.let { if (!it.isRoot) builder.appendName(it.asString()) }
    }

    override fun visitProperty(declaration: IrProperty, data: Boolean) {
        val accessor = declaration.run { getter ?: setter ?: error("Expected at least one accessor for property ${render()}") }

        isRealExpect = isRealExpect or declaration.isExpect
        typeParameterContainer.add(declaration)
        declaration.parent.accept(this, false)

        val isStaticProperty = accessor.dispatchReceiverParameter == null && declaration.parent !is IrPackageFragment

        if (isStaticProperty) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        accessor.extensionReceiverParameter?.let {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleValueParameter(builder, it)
        }

        val typeParameters = accessor.typeParameters

        typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it) }

        builder.append(declaration.name.asString())
    }

    override fun visitField(declaration: IrField, data: Boolean) =
        declaration.mangleSimpleDeclaration(declaration.name.asString())

    override fun visitEnumEntry(declaration: IrEnumEntry, data: Boolean) {
        declaration.mangleSimpleDeclaration(declaration.name.asString())
    }

    override fun visitTypeAlias(declaration: IrTypeAlias, data: Boolean) =
        declaration.mangleSimpleDeclaration(declaration.name.asString())

    override fun visitTypeParameter(declaration: IrTypeParameter, data: Boolean) {
        declaration.effectiveParent().accept(this, data)

        builder.appendSignature(MangleConstant.TYPE_PARAM_INDEX_PREFIX)
        builder.appendSignature(declaration.index)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Boolean) {
        isRealExpect = isRealExpect or declaration.isExpect

        val container = declaration.correspondingPropertySymbol?.owner ?: declaration
        val isStatic = declaration.dispatchReceiverParameter == null && container.parent !is IrPackageFragment

        declaration.mangleFunction(false, isStatic, container)
    }

    override fun visitConstructor(declaration: IrConstructor, data: Boolean) =
        declaration.mangleFunction(isCtor = true, isStatic = false, declaration)
}

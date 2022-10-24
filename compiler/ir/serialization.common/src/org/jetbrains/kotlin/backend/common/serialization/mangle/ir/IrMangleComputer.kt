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
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class IrMangleComputer(protected val builder: StringBuilder, private val mode: MangleMode, protected val compatibleMode: Boolean) :
    IrElementVisitorVoid, KotlinMangleComputer<IrDeclaration> {

    private val typeParameterContainer = ArrayList<IrDeclaration>(4)

    private var isRealExpect = false

    open fun IrFunction.platformSpecificFunctionName(): String? = null
    open fun IrFunction.platformSpecificFunctionMarks(): List<String> = emptyList()

    open fun IrFunction.specialValueParamPrefix(param: IrValueParameter): String = ""

    open fun addReturnType(): Boolean = false

    protected open fun addReturnTypeSpecialCase(irFunction: IrFunction): Boolean = false

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
        declaration.acceptVoid(this)
        return builder.toString()
    }

    private fun IrDeclaration.mangleSimpleDeclaration(name: String) {
        val l = builder.length
        parent.acceptVoid(this@IrMangleComputer)

        if (builder.length != l) builder.appendName(MangleConstant.FQN_SEPARATOR)

        builder.appendName(name)
    }

    private fun IrFunction.mangleFunction(isCtor: Boolean, isStatic: Boolean, container: IrDeclaration) {

        isRealExpect = isRealExpect or isExpect

        typeParameterContainer.add(container)
        val containerParent = container.parent
        val realParent =
            if (containerParent is IrField && containerParent.origin == IrDeclarationOrigin.DELEGATE) containerParent.parent else containerParent
        realParent.acceptVoid(this@IrMangleComputer)

        builder.appendName(MangleConstant.FUNCTION_NAME_PREFIX)

        platformSpecificFunctionName()?.let {
            builder.append(it)
            return
        }

        val funName = name.asString()

        builder.append(funName)

        mangleSignature(isCtor, isStatic)
    }

    private fun IrFunction.mangleSignature(isCtor: Boolean, isStatic: Boolean) {
        if (!mode.signature) return

        if (isStatic) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        platformSpecificFunctionMarks().forEach {
            builder.appendSignature(it)
        }

        extensionReceiverParameter?.let {
            if (!it.isHidden) {
                builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
                mangleValueParameter(builder, it)
            }
        }

        valueParameters.collectForMangler(builder, MangleConstant.VALUE_PARAMETERS) {
            if (!it.isHidden) {
                appendSignature(specialValueParamPrefix(it))
                mangleValueParameter(this, it)
            }
        }

        typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it) }

        if (!isCtor && !returnType.isUnit() && (addReturnType() || addReturnTypeSpecialCase(this))) {
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
                    is IrClassSymbol -> classifier.owner.acceptVoid(copy(MangleMode.FQNAME))
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

                //TODO
                if (type.isMarkedNullable()) tBuilder.appendSignature(MangleConstant.Q_MARK)

                mangleTypePlatformSpecific(type, tBuilder)
            }
            is IrDynamicType -> tBuilder.appendSignature(MangleConstant.DYNAMIC_MARK)
            is IrErrorType -> tBuilder.appendSignature(MangleConstant.ERROR_MARK)
            else -> error("Unexpected type $type")
        }
    }

    protected open fun mangleTypePlatformSpecific(type: IrType, tBuilder: StringBuilder) {}

    override fun visitElement(element: IrElement) =
        error("unexpected element ${element.render()}")

    override fun visitScript(declaration: IrScript) {
        declaration.parent.acceptVoid(this)
    }

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration) {
        declaration.mangleSimpleDeclaration(MangleConstant.ERROR_DECLARATION)
    }

    override fun visitClass(declaration: IrClass) {
        isRealExpect = isRealExpect or declaration.isExpect
        typeParameterContainer.add(declaration)

        val className = declaration.name.asString()
        declaration.mangleSimpleDeclaration(className)
    }

    override fun visitPackageFragment(declaration: IrPackageFragment) {
        declaration.fqName.let { if (!it.isRoot) builder.appendName(it.asString()) }
    }

    override fun visitProperty(declaration: IrProperty) {
        val accessor = declaration.run { getter ?: setter }
        require(accessor != null || declaration.backingField != null) {
            "Expected at least one accessor or backing field for property ${declaration.render()}"
        }

        isRealExpect = isRealExpect or declaration.isExpect
        typeParameterContainer.add(declaration)
        declaration.parent.acceptVoid(this)

        val isStaticProperty = if (accessor != null)
            accessor.let {
                it.dispatchReceiverParameter == null && declaration.parent !is IrPackageFragment && !declaration.parent.isFacadeClass
            }
        else {
            // Fake override for a Java field
            val backingField = declaration.resolveFakeOverride()?.backingField
                ?: error("Expected at least one accessor or a backing field for property ${declaration.render()}")
            backingField.isStatic
        }

        if (isStaticProperty) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        accessor?.extensionReceiverParameter?.let {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleValueParameter(builder, it)
        }

        val typeParameters = accessor?.typeParameters ?: emptyList()

        typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { mangleTypeParameter(this, it) }

        builder.append(declaration.name.asString())

        if (declaration.isSyntheticForJavaField) {
            builder.append(MangleConstant.JAVA_FIELD_SUFFIX)
        }
    }

    private val IrProperty.isSyntheticForJavaField: Boolean
        get() = origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && getter == null && setter == null

    override fun visitField(declaration: IrField) {
        val prop = declaration.correspondingPropertySymbol
        if (compatibleMode || prop == null) { // act as used to be (KT-48912)
            // test compiler/testData/codegen/box/ir/serializationRegressions/anonFakeOverride.kt
            declaration.mangleSimpleDeclaration(declaration.name.asString())
        } else {
            visitProperty(prop.owner)
        }
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        declaration.mangleSimpleDeclaration(declaration.name.asString())
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        val klass = declaration.parentAsClass
        val anonInitializers = klass.declarations.filterIsInstance<IrAnonymousInitializer>()

        val anonName = buildString {
            append(MangleConstant.ANON_INIT_NAME_PREFIX)
            if (anonInitializers.size > 1) {
                append(MangleConstant.LOCAL_DECLARATION_INDEX_PREFIX)
                append(anonInitializers.indexOf(declaration))
            }
        }

        declaration.mangleSimpleDeclaration(anonName)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) =
        declaration.mangleSimpleDeclaration(declaration.name.asString())

    override fun visitTypeParameter(declaration: IrTypeParameter) {
        declaration.effectiveParent().acceptVoid(this)

        builder.appendSignature(MangleConstant.TYPE_PARAM_INDEX_PREFIX)
        builder.appendSignature(declaration.index)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        isRealExpect = isRealExpect or declaration.isExpect

        val container = declaration.correspondingPropertySymbol?.owner ?: declaration
        val isStatic = declaration.dispatchReceiverParameter == null &&
                (container.parent !is IrPackageFragment && !container.parent.isFacadeClass)

        declaration.mangleFunction(false, isStatic, container)
    }

    override fun visitConstructor(declaration: IrConstructor) =
        declaration.mangleFunction(isCtor = true, isStatic = false, declaration)
}

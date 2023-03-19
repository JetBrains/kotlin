/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.ir

import org.jetbrains.kotlin.backend.common.serialization.mangle.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * A mangle computer that generates a mangled name for a Kotlin declaration represented by [IrDeclaration].
 */
open class IrMangleComputer(
    builder: StringBuilder,
    mode: MangleMode,
    protected val compatibleMode: Boolean,
    allowOutOfScopeTypeParameters: Boolean = false,
) : BaseKotlinMangleComputer<
        /*Declaration=*/IrDeclaration,
        /*Type=*/IrType,
        /*TypeParameter=*/IrTypeParameterSymbol,
        /*ValueParameter=*/IrValueParameter,
        /*TypeParameterContainer=*/IrDeclaration,
        /*FunctionDeclaration=*/IrFunction,
        /*Session=*/Nothing?,
        >(builder, mode, allowOutOfScopeTypeParameters) {

    final override fun getTypeSystemContext(session: Nothing?) = object : IrTypeSystemContext {
        override val irBuiltIns: IrBuiltIns
            get() = throw UnsupportedOperationException("Builtins are unavailable")
    }

    open fun IrFunction.platformSpecificFunctionMarks(): List<String> = emptyList()

    override fun copy(newMode: MangleMode) = IrMangleComputer(builder, newMode, compatibleMode)

    final override fun IrDeclaration.visitParent() {
        parent.acceptVoid(Visitor())
    }

    final override fun IrDeclaration.visit() {
        acceptVoid(Visitor())
    }

    private fun IrFunction.mangleFunction(isCtor: Boolean, isStatic: Boolean, container: IrDeclaration) {

        isRealExpect = isRealExpect or isExpect

        typeParameterContainers.add(container)
        val containerParent = container.parent
        val realParent =
            if (containerParent is IrField && containerParent.origin == IrDeclarationOrigin.DELEGATE) containerParent.parent else containerParent
        realParent.acceptVoid(Visitor())

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
                mangleValueParameter(builder, it, null)
            }
        }

        valueParameters.collectForMangler(builder, MangleConstant.VALUE_PARAMETERS) {
            if (!it.isHidden) {
                appendSignature(specialValueParamPrefix(it))
                mangleValueParameter(this, it, null)
            }
        }

        typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) {
            mangleTypeParameter(this, it.symbol, it.index, null)
        }

        if (!isCtor && !returnType.isUnit() && (addReturnType() || addReturnTypeSpecialCase(this))) {
            mangleType(builder, returnType, null)
        }
    }

    final override fun getEffectiveParent(typeParameter: IrTypeParameterSymbol): IrDeclaration = typeParameter.owner.run {
        when (val irParent = parent) {
            is IrSimpleFunction -> irParent.correspondingPropertySymbol?.owner ?: irParent
            is IrTypeParametersContainer -> irParent
            else -> error("Unexpected type parameter container ${irParent.render()} for TP ${render()}")
        }
    }

    override fun renderDeclaration(declaration: IrDeclaration) = declaration.render()

    override fun getTypeParameterName(typeParameter: IrTypeParameterSymbol) = typeParameter.owner.name.asString()

    final override fun isVararg(valueParameter: IrValueParameter) = valueParameter.varargElementType != null

    final override fun getValueParameterType(valueParameter: IrValueParameter) = valueParameter.type

    final override fun getIndexOfTypeParameter(typeParameter: IrTypeParameterSymbol, container: IrDeclaration) = typeParameter.owner.index

    final override fun mangleType(tBuilder: StringBuilder, type: IrType, declarationSiteSession: Nothing?) {
        when (type) {
            is IrSimpleType -> {
                when (val classifier = type.classifier) {
                    is IrClassSymbol -> with(copy(MangleMode.FQNAME)) { classifier.owner.visit() }
                    is IrTypeParameterSymbol -> tBuilder.mangleTypeParameterReference(classifier)
                }

                mangleTypeArguments(tBuilder, type, null)

                //TODO
                if (type.isMarkedNullable()) tBuilder.appendSignature(MangleConstant.Q_MARK)

                mangleTypePlatformSpecific(type, tBuilder)
            }
            is IrDynamicType -> tBuilder.appendSignature(MangleConstant.DYNAMIC_MARK)
            is IrErrorType -> tBuilder.appendSignature(MangleConstant.ERROR_MARK)
            else -> error("Unexpected type $type")
        }
    }

    private inner class Visitor : IrElementVisitorVoid {

        override fun visitElement(element: IrElement) =
            error("unexpected element ${element.render()}")

        override fun visitScript(declaration: IrScript) {
            declaration.visitParent()
        }

        override fun visitErrorDeclaration(declaration: IrErrorDeclaration) {
            declaration.mangleSimpleDeclaration(MangleConstant.ERROR_DECLARATION)
        }

        override fun visitClass(declaration: IrClass) {
            isRealExpect = isRealExpect or declaration.isExpect
            typeParameterContainers.add(declaration)

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
            typeParameterContainers.add(declaration)
            declaration.visitParent()

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
                mangleValueParameter(builder, it, null)
            }

            val typeParameters = accessor?.typeParameters ?: emptyList()

            typeParameters.collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) {
                mangleTypeParameter(this, it.symbol, it.index, null)
            }

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
            getEffectiveParent(declaration.symbol).visit()

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
}

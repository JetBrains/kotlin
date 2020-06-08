/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.DeclarationFactory
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name

class JsSharedVariablesManager(val builtIns: IrBuiltIns, val implicitDeclarationsFile: IrPackageFragment) : SharedVariablesManager {

    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        val valueType = originalDeclaration.type
        val initializer = originalDeclaration.initializer ?: IrConstImpl.constNull(
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            valueType
        )

        val constructorSymbol = closureBoxConstructorDeclaration.symbol

        val irCall =
            IrConstructorCallImpl(
                initializer.startOffset, initializer.endOffset,
                closureBoxType, constructorSymbol,
                valueArgumentsCount = 1,
                typeArgumentsCount = 0,
                constructorTypeArgumentsCount = 0
            ).apply {
                putValueArgument(0, initializer)
            }

        val descriptor = WrappedVariableDescriptor()
        return IrVariableImpl(
            originalDeclaration.startOffset,
            originalDeclaration.endOffset,
            originalDeclaration.origin,
            IrVariableSymbolImpl(descriptor),
            originalDeclaration.name,
            irCall.type,
            isVar = false,
            isConst = false,
            isLateinit = false
        ).also {
            descriptor.bind(it)
            it.parent = originalDeclaration.parent
            it.initializer = irCall
        }
    }

    override fun defineSharedValue(originalDeclaration: IrVariable, sharedVariableDeclaration: IrVariable) = sharedVariableDeclaration

    override fun getSharedValue(sharedVariableSymbol: IrVariableSymbol, originalGet: IrGetValue) = IrGetFieldImpl(
        originalGet.startOffset, originalGet.endOffset,
        closureBoxFieldDeclaration.symbol,
        originalGet.type,
        IrGetValueImpl(
            originalGet.startOffset,
            originalGet.endOffset,
            closureBoxType,
            sharedVariableSymbol,
            originalGet.origin
        ),
        originalGet.origin
    )

    override fun setSharedValue(sharedVariableSymbol: IrVariableSymbol, originalSet: IrSetVariable): IrExpression =
        IrSetFieldImpl(
            originalSet.startOffset,
            originalSet.endOffset,
            closureBoxFieldDeclaration.symbol,
            IrGetValueImpl(
                originalSet.startOffset,
                originalSet.endOffset,
                closureBoxType,
                sharedVariableSymbol,
                originalSet.origin
            ),
            originalSet.value,
            originalSet.type,
            originalSet.origin
        )

    private val boxTypeName = "\$closureBox\$"

    private val closureBoxClassDeclaration by lazy {
        createClosureBoxClassDeclaration()
    }

    private val closureBoxConstructorDeclaration by lazy {
        createClosureBoxConstructorDeclaration()
    }

    private val closureBoxFieldDeclaration by lazy {
        closureBoxPropertyDeclaration
    }

    private val closureBoxPropertyDeclaration by lazy {
        createClosureBoxPropertyDeclaration()
    }

    private lateinit var closureBoxType: IrType

    private fun createClosureBoxClassDeclaration(): IrClass {
        val descriptor = WrappedClassDescriptor()
        val declaration = IrClassImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, JsLoweredDeclarationOrigin.JS_CLOSURE_BOX_CLASS_DECLARATION, IrClassSymbolImpl(descriptor),
            Name.identifier(boxTypeName), ClassKind.CLASS, Visibilities.PUBLIC, Modality.FINAL
        )

        descriptor.bind(declaration)
        declaration.parent = implicitDeclarationsFile
        // TODO: substitute
        closureBoxType = IrSimpleTypeImpl(declaration.symbol, false, emptyList(), emptyList())
        declaration.thisReceiver =
            JsIrBuilder.buildValueParameter(Name.special("<this>"), -1, closureBoxType, IrDeclarationOrigin.INSTANCE_RECEIVER).apply {
                parent = declaration
            }
        implicitDeclarationsFile.declarations += declaration

        return declaration
    }

    private fun createClosureBoxPropertyDeclaration(): IrField {
        val descriptor = WrappedFieldDescriptor()
        val symbol = IrFieldSymbolImpl(descriptor)
        val fieldName = Name.identifier("v")
        return IrFieldImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            DeclarationFactory.FIELD_FOR_OUTER_THIS,
            symbol,
            fieldName,
            builtIns.anyNType,
            Visibilities.PUBLIC,
            isFinal = false,
            isExternal = false,
            isStatic = false,
            isFakeOverride = false
        ).also {
            descriptor.bind(it)
            it.parent = closureBoxClassDeclaration
            closureBoxClassDeclaration.declarations += it
        }
    }

    private fun createClosureBoxConstructorDeclaration(): IrConstructor {
        val descriptor = WrappedClassConstructorDescriptor()
        val symbol = IrConstructorSymbolImpl(descriptor)

        val declaration = IrConstructorImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, JsLoweredDeclarationOrigin.JS_CLOSURE_BOX_CLASS_DECLARATION, symbol,
            Name.special("<init>"), Visibilities.PUBLIC, closureBoxClassDeclaration.defaultType,
            isInline = false, isExternal = false, isPrimary = true, isExpect = false
        )

        descriptor.bind(declaration)
        declaration.parent = closureBoxClassDeclaration

        val parameterDeclaration = createClosureBoxConstructorParameterDeclaration(declaration)

        declaration.valueParameters += parameterDeclaration

        val receiver = JsIrBuilder.buildGetValue(closureBoxClassDeclaration.thisReceiver!!.symbol)
        val value = JsIrBuilder.buildGetValue(parameterDeclaration.symbol)

        val setField = JsIrBuilder.buildSetField(closureBoxFieldDeclaration.symbol, receiver, value, closureBoxFieldDeclaration.type)

        declaration.body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(setField))

        closureBoxClassDeclaration.declarations += declaration
        return declaration
    }

    private fun createClosureBoxConstructorParameterDeclaration(irConstructor: IrConstructor): IrValueParameter {
        return JsIrBuilder.buildValueParameter("p", 0, closureBoxPropertyDeclaration.type).also {
            it.parent = irConstructor
        }
    }
}

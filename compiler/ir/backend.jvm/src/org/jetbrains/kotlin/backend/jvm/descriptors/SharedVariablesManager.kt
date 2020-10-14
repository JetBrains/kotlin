/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JvmSharedVariablesManager(
    module: ModuleDescriptor,
    val symbols: JvmSymbols,
    val irBuiltIns: IrBuiltIns,
    irFactory: IrFactory,
) : SharedVariablesManager {
    private val jvmInternalPackage = IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
        module, FqName("kotlin.jvm.internal")
    )

    private val refNamespaceClass = irFactory.addClass(jvmInternalPackage) {
        name = Name.identifier("Ref")
    }

    private class RefProvider(val refClass: IrClass, elementType: IrType) {
        val refConstructor = refClass.addConstructor {
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
        }

        val elementField = refClass.addField {
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            name = Name.identifier("element")
            type = elementType
        }
    }

    private val primitiveRefProviders = irBuiltIns.primitiveIrTypes.associate { primitiveType ->
        val refClass = irFactory.addClass(refNamespaceClass) {
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            name = Name.identifier(primitiveType.classOrNull!!.owner.name.asString() + "Ref")
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
        }
        primitiveType.classifierOrFail to RefProvider(refClass, primitiveType)
    }

    private val objectRefProvider = run {
        val refClass = irFactory.addClass(refNamespaceClass) {
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            name = Name.identifier("ObjectRef")
        }.apply {
            addTypeParameter {
                name = Name.identifier("T")
                superTypes.add(irBuiltIns.anyNType)
            }
            createImplicitParameterDeclarationWithWrappedDescriptor()
        }
        RefProvider(refClass, refClass.typeParameters[0].defaultType)
    }

    private fun getProvider(valueType: IrType): RefProvider =
        if (valueType.isPrimitiveType())
            primitiveRefProviders.getValue(valueType.classifierOrFail)
        else
            objectRefProvider

    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        val valueType = originalDeclaration.type
        val provider = getProvider(InlineClassAbi.unboxType(valueType) ?: valueType)
        val typeArguments = provider.refClass.typeParameters.map { valueType }
        val refType = provider.refClass.typeWith(typeArguments)
        val refConstructorCall = IrConstructorCallImpl.fromSymbolOwner(
            originalDeclaration.startOffset, originalDeclaration.startOffset, refType, provider.refConstructor.symbol
        ).apply {
            typeArguments.forEachIndexed(::putTypeArgument)
        }
        return with(originalDeclaration) {
            IrVariableImpl(
                startOffset, endOffset, origin, IrVariableSymbolImpl(WrappedVariableDescriptor()), name, refType,
                isVar = false, // writes are remapped to field stores
                isConst = false, // const vals could not possibly require ref wrappers
                isLateinit = false
            ).apply {
                (descriptor as WrappedVariableDescriptor).bind(this)
                initializer = refConstructorCall
            }
        }
    }

    override fun defineSharedValue(originalDeclaration: IrVariable, sharedVariableDeclaration: IrVariable): IrStatement {
        val initializer = originalDeclaration.initializer ?: return sharedVariableDeclaration
        val default = IrConstImpl.defaultValueForType(initializer.startOffset, initializer.endOffset, originalDeclaration.type)
        if (initializer is IrConst<*> && initializer.value == default.value) {
            // The field is preinitialized to the default value, so an explicit set is not required.
            return sharedVariableDeclaration
        }
        val initializationStatement = with (originalDeclaration) {
            IrSetValueImpl(startOffset, endOffset, irBuiltIns.unitType, symbol, initializer, null)
        }
        val sharedVariableInitialization = setSharedValue(sharedVariableDeclaration.symbol, initializationStatement)
        return with(originalDeclaration) {
            IrCompositeImpl(
                startOffset, endOffset, irBuiltIns.unitType, null,
                listOf(sharedVariableDeclaration, sharedVariableInitialization)
            )
        }
    }

    private fun unsafeCoerce(value: IrExpression, from: IrType, to: IrType): IrExpression =
        IrCallImpl.fromSymbolOwner(value.startOffset, value.endOffset, to, symbols.unsafeCoerceIntrinsic).apply {
            putTypeArgument(0, from)
            putTypeArgument(1, to)
            putValueArgument(0, value)
        }

    override fun getSharedValue(sharedVariableSymbol: IrVariableSymbol, originalGet: IrGetValue): IrExpression =
        with(originalGet) {
            val unboxedType = InlineClassAbi.unboxType(symbol.owner.type)
            val provider = getProvider(unboxedType ?: symbol.owner.type)
            val receiver = IrGetValueImpl(startOffset, endOffset, sharedVariableSymbol)
            val unboxedRead = IrGetFieldImpl(startOffset, endOffset, provider.elementField.symbol, unboxedType ?: type, receiver, origin)
            unboxedType?.let { unsafeCoerce(unboxedRead, it, symbol.owner.type) } ?: unboxedRead
        }

    override fun setSharedValue(sharedVariableSymbol: IrVariableSymbol, originalSet: IrSetValue): IrExpression =
        with(originalSet) {
            val unboxedType = InlineClassAbi.unboxType(symbol.owner.type)
            val unboxedValue = unboxedType?.let { unsafeCoerce(value, symbol.owner.type, it) } ?: value
            val provider = getProvider(unboxedType ?: symbol.owner.type)
            val receiver = IrGetValueImpl(startOffset, endOffset, sharedVariableSymbol)
            IrSetFieldImpl(startOffset, endOffset, provider.elementField.symbol, receiver, unboxedValue, type, origin)
        }
}

private inline fun IrFactory.addClass(
    container: IrDeclarationContainer,
    builder: IrClassBuilder.() -> Unit
): IrClass = buildClass(builder).also {
    it.parent = container
    container.declarations += it
}

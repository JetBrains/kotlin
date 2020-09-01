/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.ir.addFakeOverridesViaIncorrectHeuristic
import org.jetbrains.kotlin.backend.common.ir.addSimpleDelegatingConstructor
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name


internal object DECLARATION_ORIGIN_ENUM : IrDeclarationOriginImpl("ENUM")

internal data class LoweredEnum(val implObject: IrClass,
                                val valuesField: IrField,
                                val valuesGetter: IrSimpleFunction,
                                val itemGetterSymbol: IrSimpleFunctionSymbol,
                                val entriesMap: Map<Name, Int>)

internal class EnumSpecialDeclarationsFactory(val context: Context) {
    private val symbols = context.ir.symbols

    fun createLoweredEnum(enumClass: IrClass): LoweredEnum {
        val startOffset = enumClass.startOffset
        val endOffset = enumClass.endOffset

        val implObject = WrappedClassDescriptor().let {
            IrClassImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_ENUM,
                    IrClassSymbolImpl(it),
                    "OBJECT".synthesizedName,
                    ClassKind.OBJECT,
                    DescriptorVisibilities.PUBLIC,
                    Modality.FINAL,
                    isCompanion = false,
                    isInner = false,
                    isData = false,
                    isExternal = false,
                    isInline = false,
                    isExpect = false,
                    isFun = false
            ).apply {
                it.bind(this)
                parent = enumClass
                createParameterDeclarations()
            }
        }

        val valuesType = symbols.array.typeWith(enumClass.defaultType)
        val valuesField = WrappedFieldDescriptor().let {
            IrFieldImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_ENUM,
                    IrFieldSymbolImpl(it),
                    "VALUES".synthesizedName,
                    valuesType,
                    DescriptorVisibilities.PRIVATE,
                    isFinal = true,
                    isExternal = false,
                    isStatic = false,
            ).apply {
                it.bind(this)
                parent = implObject
            }
        }

        val valuesGetter = WrappedSimpleFunctionDescriptor().let {
            IrFunctionImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_ENUM,
                    IrSimpleFunctionSymbolImpl(it),
                    "get-VALUES".synthesizedName,
                    DescriptorVisibilities.PUBLIC,
                    Modality.FINAL,
                    valuesType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = false,
                    isExpect = false,
                    isFakeOverride = false,
                    isOperator = false,
                    isInfix = false
            ).apply {
                it.bind(this)
                parent = implObject
            }
        }

        val constructorOfAny = context.irBuiltIns.anyClass.owner.constructors.first()
        implObject.addSimpleDelegatingConstructor(
                constructorOfAny,
                context.irBuiltIns,
                true // TODO: why primary?
        )

        implObject.superTypes += context.irBuiltIns.anyType
        implObject.addFakeOverridesViaIncorrectHeuristic()

        val itemGetterSymbol = symbols.array.functions.single { it.descriptor.name == Name.identifier("get") }
        val enumEntriesMap = enumClass.declarations
                .filterIsInstance<IrEnumEntry>()
                .sortedBy { it.name }
                .withIndex()
                .associate { it.value.name to it.index }
                .toMap()

        return LoweredEnum(
                implObject,
                valuesField,
                valuesGetter,
                itemGetterSymbol,
                enumEntriesMap)
    }
}

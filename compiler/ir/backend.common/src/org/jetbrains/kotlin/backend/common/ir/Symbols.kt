/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.getValue

@OptIn(InternalSymbolFinderAPI::class)
abstract class Symbols(irBuiltIns: IrBuiltIns) : PreSerializationSymbols.Impl(irBuiltIns) {
    val iterator: IrClassSymbol = irBuiltIns.iteratorClass

    val charSequence: IrClassSymbol = irBuiltIns.charSequenceClass
    val string: IrClassSymbol = irBuiltIns.stringClass

    val primitiveIteratorsByType = mapOf(
        PrimitiveType.BOOLEAN to irBuiltIns.booleanIterator,
        PrimitiveType.CHAR to irBuiltIns.charIterator,
        PrimitiveType.BYTE to irBuiltIns.byteIterator,
        PrimitiveType.SHORT to irBuiltIns.shortIterator,
        PrimitiveType.INT to irBuiltIns.intIterator,
        PrimitiveType.FLOAT to irBuiltIns.floatIterator,
        PrimitiveType.LONG to irBuiltIns.longIterator,
        PrimitiveType.DOUBLE to irBuiltIns.doubleIterator,
    )

    // The "...OrNull" variants are used for the classes below because the minimal stdlib used in tests do not include those classes.
    // It was not feasible to add them to the JS reduced runtime because all its transitive dependencies also need to be
    // added, which would include a lot of the full stdlib.
    val uIntProgression: IrClassSymbol? = ClassIds.UIntProgression.classSymbolOrNull()
    val uLongProgression: IrClassSymbol? = ClassIds.ULongProgression.classSymbolOrNull()
    val uIntRange: IrClassSymbol? = ClassIds.UIntRange.classSymbolOrNull()
    val uLongRange: IrClassSymbol? = ClassIds.ULongRange.classSymbolOrNull()
    val sequence: IrClassSymbol? = ClassIds.Sequence.classSymbolOrNull()

    val charProgression: IrClassSymbol = ClassIds.CharProgression.classSymbol()
    val intProgression: IrClassSymbol = ClassIds.IntProgression.classSymbol()
    val longProgression: IrClassSymbol = ClassIds.LongProgression.classSymbol()
    val progressionClasses = listOfNotNull(charProgression, intProgression, longProgression, uIntProgression, uLongProgression)

    val charRange: IrClassSymbol = ClassIds.CharRange.classSymbol()
    val intRange: IrClassSymbol = ClassIds.IntRange.classSymbol()
    val longRange: IrClassSymbol = ClassIds.LongRange.classSymbol()
    val rangeClasses = listOfNotNull(charRange, intRange, longRange, uIntRange, uLongRange)

    val closedRange: IrClassSymbol = ClassIds.ClosedRange.classSymbol()

    abstract val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>

    val toUIntByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by CallableIds.toUIntExtension.functionSymbolAssociatedBy {
        it.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type?.classifierOrFail
            ?: error("Expected extension receiver for ${it.render()}")
    }

    val toULongByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by CallableIds.toULongExtension.functionSymbolAssociatedBy {
        it.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type?.classifierOrFail
            ?: error("Expected extension receiver for ${it.render()}")
    }

    val unit get() = irBuiltIns.unitClass

    val char get() = irBuiltIns.charClass

    val byte get() = irBuiltIns.byteClass
    val short get() = irBuiltIns.shortClass
    val int get() = irBuiltIns.intClass
    val long get() = irBuiltIns.longClass
    val float get() = irBuiltIns.floatClass
    val double get() = irBuiltIns.doubleClass

    val integerClasses = listOf(byte, short, int, long)

    val progressionElementTypes: Collection<IrType> by lazy {
        listOfNotNull(
            byte, short, int, long, char,
            irBuiltIns.ubyteClass, irBuiltIns.ushortClass, irBuiltIns.uintClass, irBuiltIns.ulongClass
        ).map { it.defaultType }
    }

    val arrayOf: IrSimpleFunctionSymbol get() = irBuiltIns.arrayOf
    val arrayOfNulls: IrSimpleFunctionSymbol get() = irBuiltIns.arrayOfNulls

    val array get() = irBuiltIns.arrayClass

    val primitiveTypesToPrimitiveArrays get() = irBuiltIns.primitiveTypesToPrimitiveArrays
    val primitiveArraysToPrimitiveTypes get() = irBuiltIns.primitiveArraysToPrimitiveTypes
    val unsignedTypesToUnsignedArrays get() = irBuiltIns.unsignedTypesToUnsignedArrays

    open fun functionN(n: Int): IrClassSymbol = irBuiltIns.functionN(n).symbol
    open fun suspendFunctionN(n: Int): IrClassSymbol = irBuiltIns.suspendFunctionN(n).symbol

    val extensionToString: IrSimpleFunctionSymbol by CallableIds.extensionToString.functionSymbol {
        it.hasShape(extensionReceiver = true, parameterTypes = listOf(irBuiltIns.anyNType))
    }
    val memberToString: IrSimpleFunctionSymbol = CallableIds.memberToString.functionSymbol()
    val extensionStringPlus: IrSimpleFunctionSymbol by CallableIds.extensionStringPlus.functionSymbol {
        it.hasShape(
            extensionReceiver = true,
            regularParameters = 1,
            parameterTypes = listOf(irBuiltIns.stringType.makeNullable(), irBuiltIns.anyNType)
        )
    }
    val memberStringPlus: IrSimpleFunctionSymbol = CallableIds.memberPlus.functionSymbol()

    fun isStringPlus(functionSymbol: IrFunctionSymbol): Boolean {
        val plusSymbol = when {
            functionSymbol.owner.hasShape(
                dispatchReceiver = true,
                regularParameters = 1,
                parameterTypes = listOf(irBuiltIns.stringType, null)
            ) -> irBuiltIns.memberStringPlus
            functionSymbol.owner.hasShape(
                extensionReceiver = true,
                regularParameters = 1,
                parameterTypes = listOf(irBuiltIns.stringType.makeNullable(), null)
            ) -> irBuiltIns.extensionStringPlus
            else -> return false
        }

        return functionSymbol == plusSymbol
    }

    abstract val throwNullPointerException: IrSimpleFunctionSymbol
    abstract val throwTypeCastException: IrSimpleFunctionSymbol

    abstract val throwKotlinNothingValueException: IrSimpleFunctionSymbol

    open val throwISE: IrSimpleFunctionSymbol
        get() = error("throwISE is not implemented")

    open val throwIAE: IrSimpleFunctionSymbol
        get() = error("throwIAE is not implemented")

    abstract val stringBuilder: IrClassSymbol

    abstract val coroutineImpl: IrClassSymbol

    abstract val coroutineSuspendedGetter: IrSimpleFunctionSymbol

    abstract val getContinuation: IrSimpleFunctionSymbol

    abstract val continuationClass: IrClassSymbol

    abstract val returnIfSuspended: IrSimpleFunctionSymbol

    abstract val functionAdapter: IrClassSymbol

    abstract val defaultConstructorMarker: IrClassSymbol

    open val unsafeCoerceIntrinsic: IrSimpleFunctionSymbol?
        get() = null

    open val getWithoutBoundCheckName: Name?
        get() = null

    open val setWithoutBoundCheckName: Name?
        get() = null

    /**
     * Determines whether the provided function call is free of side effects.
     * If it is, then we consider this function to be pure and that unblocks some backend optimizations.
     */
    open fun isSideEffectFree(call: IrCall): Boolean {
        return false
    }
}

// TODO KT-77388 rename to `BackendKlibSymbolsImpl`
@OptIn(InternalSymbolFinderAPI::class)
abstract class KlibSymbols(irBuiltIns: IrBuiltIns) : PreSerializationKlibSymbols, Symbols(irBuiltIns) {
    final override val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by CallableId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("getProgressionLastElement")).functionSymbolAssociatedBy {
        it.returnType.classifierOrFail
    }

    val primitiveSharedVariableBoxes: Map<IrType, PreSerializationKlibSymbols.SharedVariableBoxClassInfo> = PrimitiveType.entries.associate {
        irBuiltIns.primitiveTypeToIrType[it]!! to findSharedVariableBoxClass(it)
    }

    val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol> by contentEquals.functionSymbolAssociatedBy(
        condition = { it.hasShape(extensionReceiver = true, regularParameters = 1) && it.parameters[0].type.isNullable() },
        getKey = { it.parameters[0].type.makeNotNull() }
    )

    companion object {
        private val String.collectionsCallableId get() = CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier(this))
        private val contentEquals = "contentEquals".collectionsCallableId
    }
}

private object ClassIds {
    private val String.rangesClassId get() = ClassId(StandardNames.RANGES_PACKAGE_FQ_NAME, Name.identifier(this))
    val CharProgression = "CharProgression".rangesClassId
    val IntProgression = "IntProgression".rangesClassId
    val LongProgression = "LongProgression".rangesClassId
    val CharRange = "CharRange".rangesClassId
    val IntRange = "IntRange".rangesClassId
    val LongRange = "LongRange".rangesClassId
    val ClosedRange = "ClosedRange".rangesClassId
    val UIntProgression = "UIntProgression".rangesClassId
    val ULongProgression = "ULongProgression".rangesClassId
    val UIntRange = "UIntRange".rangesClassId
    val ULongRange = "ULongRange".rangesClassId

    private val String.sequencesClassId get() = ClassId(StandardClassIds.BASE_SEQUENCES_PACKAGE, Name.identifier(this))
    val Sequence = "Sequence".sequencesClassId
}

private object CallableIds {
    private val String.baseCallableId get() = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(this))
    val toUIntExtension = "toUInt".baseCallableId
    val toULongExtension = "toULong".baseCallableId

    val extensionToString = OperatorNameConventions.TO_STRING.toString().baseCallableId
    val extensionStringPlus = OperatorNameConventions.PLUS.toString().baseCallableId
    val memberToString = CallableId(StandardClassIds.Any, Name.identifier(OperatorNameConventions.TO_STRING.toString()))
    val memberPlus = CallableId(StandardClassIds.String, Name.identifier(OperatorNameConventions.PLUS.toString()))
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend

import org.jetbrains.kotlin.bir.BirBuiltIns
import org.jetbrains.kotlin.bir.BirForest
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirSimpleFunction
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.name.Name

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class BirBuiltInSymbols constructor(
    protected val birBuiltIns: BirBuiltIns,
    protected val birForest: BirForest,
    protected val converter: Ir2BirConverter,
) {
    val iterator = getClass(Name.identifier("Iterator"), "kotlin", "collections")

    val charSequence = getClass(Name.identifier("CharSequence"), "kotlin")
    val string = getClass(Name.identifier("String"), "kotlin")

    val primitiveIteratorsByType = PrimitiveType.values().associate { type ->
        val iteratorClass = getClass(Name.identifier(type.typeName.asString() + "Iterator"), "kotlin", "collections")
        type to iteratorClass
    }

    val asserts = birBuiltIns.findFunctions(Name.identifier("assert"), "kotlin")

    private fun progression(name: String) = getClass(Name.identifier(name), "kotlin", "ranges")
    private fun progressionOrNull(name: String) = findClass(Name.identifier(name), "kotlin", "ranges")

    // The "...OrNull" variants are used for the classes below because the minimal stdlib used in tests do not include those classes.
    // It was not feasible to add them to the JS reduced runtime because all its transitive dependencies also need to be
    // added, which would include a lot of the full stdlib.
    open val uByte = findClass(Name.identifier("UByte"), "kotlin")
    open val uShort = findClass(Name.identifier("UShort"), "kotlin")
    open val uInt = findClass(Name.identifier("UInt"), "kotlin")
    open val uLong = findClass(Name.identifier("ULong"), "kotlin")
    val uIntProgression = progressionOrNull("UIntProgression")
    val uLongProgression = progressionOrNull("ULongProgression")
    val uIntRange = progressionOrNull("UIntRange")
    val uLongRange = progressionOrNull("ULongRange")
    val sequence = findClass(Name.identifier("Sequence"), "kotlin", "sequences")

    val charProgression = progression("CharProgression")
    val intProgression = progression("IntProgression")
    val longProgression = progression("LongProgression")
    val progressionClasses = listOfNotNull(charProgression, intProgression, longProgression, uIntProgression, uLongProgression)

    val charRange = progression("CharRange")
    val intRange = progression("IntRange")
    val longRange = progression("LongRange")
    val rangeClasses = listOfNotNull(charRange, intRange, longRange, uIntRange, uLongRange)

    val closedRange = progression("ClosedRange")

    // todo: those use IrClassifierSymbol
    /*open val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        irBuiltIns.getNonBuiltinFunctionsByReturnType(Name.identifier("getProgressionLastElement"), "kotlin", "internal")
    open val toUIntByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        irBuiltIns.getNonBuiltInFunctionsByExtensionReceiver(Name.identifier("toUInt"), "kotlin")
    open val toULongByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        irBuiltIns.getNonBuiltInFunctionsByExtensionReceiver(Name.identifier("toULong"), "kotlin")*/


    abstract val throwNullPointerException: BirSimpleFunction
    abstract val throwTypeCastException: BirSimpleFunction

    abstract val throwUninitializedPropertyAccessException: BirSimpleFunction

    abstract val throwKotlinNothingValueException: BirSimpleFunction

    open val throwISE: BirSimpleFunction
        get() = error("throwISE is not implemented")

    abstract val stringBuilder: BirClass

    abstract val defaultConstructorMarker: BirClass

    abstract val coroutineImpl: BirClass

    abstract val coroutineSuspendedGetter: BirSimpleFunction

    abstract val getContinuation: BirSimpleFunction

    abstract val continuationClass: BirClass

    abstract val coroutineContextGetter: BirSimpleFunction

    abstract val suspendCoroutineUninterceptedOrReturn: BirSimpleFunction

    abstract val coroutineGetContext: BirSimpleFunction

    abstract val returnIfSuspended: BirSimpleFunction

    abstract val functionAdapter: BirClass

    open val unsafeCoerceIntrinsic: BirSimpleFunction? = null

    open val getWithoutBoundCheckName: Name? = null

    open val setWithoutBoundCheckName: Name? = null

    open val arraysContentEquals: Map<BirType, BirSimpleFunction>? = null


    protected fun findClass(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): BirClass? {
        return birBuiltIns.findClass(name, *packageNameSegments)
    }

    protected fun getClass(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): BirClass =
        findClass(name, *packageNameSegments)
            ?: error("Class '$name' not found in package '${packageNameSegments.joinToString(".")}'")
}
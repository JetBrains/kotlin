/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.unsigned

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.convert
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.BasePrimitivesGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.END_LINE
import org.jetbrains.kotlin.generators.builtins.printDoc
import java.io.File
import java.io.PrintWriter

fun generateUnsignedTypes(
    targetDir: File,
    generate: (File, (PrintWriter) -> BuiltInsSourceGenerator) -> Unit
) {
    for (type in UnsignedType.entries) {
        generate(File(targetDir, "kotlin/${type.capitalized}.kt")) { UnsignedTypeGenerator(type, it) }
        generate(File(targetDir, "kotlin/${type.capitalized}Array.kt")) { UnsignedArrayGenerator(type, it) }
    }

    for (type in listOf(UnsignedType.UINT, UnsignedType.ULONG)) {
        generate(File(targetDir, "kotlin/${type.capitalized}Range.kt")) { UnsignedRangeGenerator(type, it) }
    }
}

class UnsignedTypeGenerator(val type: UnsignedType, out: PrintWriter) : BuiltInsSourceGenerator(out) {
    private val className = type.capitalized
    private val storageType = type.asSigned.capitalized

    internal fun binaryOperatorDoc(operator: String, operand1: UnsignedType, operand2: UnsignedType): String = when (operator) {
        "floorDiv" ->
            """
            Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
            
            For unsigned types, the results of flooring division and truncating division are the same.
            """.trimIndent()
        "rem" -> {
            """
                Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
                
                The result is always less than the divisor.
                """.trimIndent()
        }
        "mod" -> {
            """
                Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).

                The result is always less than the divisor.
                
                For unsigned types, the remainders of flooring division and truncating division are the same.
                """.trimIndent()
        }
        else -> BasePrimitivesGenerator.binaryOperatorDoc(operator, operand1.asSigned, operand2.asSigned)
    }

    override fun generateBody() {

        out.println("import kotlin.experimental.*")
        out.println("import kotlin.jvm.*")
        out.println()

        out.println("@SinceKotlin(\"1.5\")")
        out.println("@WasExperimental(ExperimentalUnsignedTypes::class)")
        out.println("@JvmInline")
        out.println("public value class $className @kotlin.internal.IntrinsicConstEvaluation @PublishedApi internal constructor(@PublishedApi internal val data: $storageType) : Comparable<$className> {")
        out.println()
        out.println("""    public companion object {
        /**
         * A constant holding the minimum value an instance of $className can have.
         */
        public const val MIN_VALUE: $className = $className(0)

        /**
         * A constant holding the maximum value an instance of $className can have.
         */
        public const val MAX_VALUE: $className = $className(-1)

        /**
         * The number of bytes used to represent an instance of $className in a binary form.
         */
        public const val SIZE_BYTES: Int = ${type.byteSize}

        /**
         * The number of bits used to represent an instance of $className in a binary form.
         */
        public const val SIZE_BITS: Int = ${type.byteSize * 8}
    }""")

        generateCompareTo()

        generateBinaryOperators()
        generateUnaryOperators()
        generateRangeTo()
        generateRangeUntil()

        if (type == UnsignedType.UINT || type == UnsignedType.ULONG) {
            generateBitShiftOperators()
        }

        generateBitwiseOperators()

        generateMemberConversions()
        generateFloatingConversions()

        generateToStringHashCode()

        out.println("}")
        out.println()


        generateExtensionConversions()
    }


    private fun generateCompareTo() {
        for (otherType in UnsignedType.entries) {
            out.println("""
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */""")
            out.println("    @kotlin.internal.InlineOnly")
            if (otherType == type)
                out.println("""    @Suppress("OVERRIDE_BY_INLINE")""")
            out.print("    public ")
            if (otherType == type) out.print("override ")
            out.print("inline operator fun compareTo(other: ${otherType.capitalized}): Int = ")
            if (otherType == type && maxByDomainCapacity(type, UnsignedType.UINT) == type) {
                out.println("${className.lowercase()}Compare(this.data, other.data)")
            } else {
                if (maxOf(type, otherType) < UnsignedType.UINT) {
                    out.println("this.toInt().compareTo(other.toInt())")
                } else {
                    val ctype = maxByDomainCapacity(type, otherType)
                    out.println("${convert("this", type, ctype)}.compareTo(${convert("other", otherType, ctype)})")
                }
            }
        }
        out.println()
    }

    private fun generateBinaryOperators() {
        for (name in BasePrimitivesGenerator.binaryOperators) {
            generateOperator(name)
        }
        generateFloorDivMod("floorDiv")
        generateFloorDivMod("mod")
    }

    private fun generateOperator(name: String) {
        for (otherType in UnsignedType.entries) {
            val returnType = getOperatorReturnType(type, otherType)

            out.printDoc(binaryOperatorDoc(name, type, otherType), "    ")
            out.println("    @kotlin.internal.InlineOnly")
            out.print("    public inline operator fun $name(other: ${otherType.capitalized}): ${returnType.capitalized} = ")
            if (type == otherType && type == returnType) {
                when (name) {
                    "plus", "minus", "times" -> out.println("$className(this.data.$name(other.data))")
                    "div" -> out.println("${type.capitalized.lowercase()}Divide(this, other)")
                    "rem" -> out.println("${type.capitalized.lowercase()}Remainder(this, other)")
                    else -> error(name)
                }
            } else {
                out.println("${convert("this", type, returnType)}.$name(${convert("other", otherType, returnType)})")
            }
        }
        out.println()
    }

    private fun generateFloorDivMod(name: String) {
        for (otherType in UnsignedType.entries) {
            val operationType = getOperatorReturnType(type, otherType)
            val returnType = if (name == "mod") otherType else operationType

            out.printDoc(binaryOperatorDoc(name, type, otherType), "    ")
            out.println("    @kotlin.internal.InlineOnly")
            out.print("    public inline fun $name(other: ${otherType.capitalized}): ${returnType.capitalized} = ")
            if (type == otherType && type == operationType) {
                when (name) {
                    "floorDiv" -> out.println("div(other)")
                    "mod" -> out.println("rem(other)")
                    else -> error(name)
                }
            } else {
                out.println(
                    convert(
                        "${convert("this", type, operationType)}.$name(${convert("other", otherType, operationType)})",
                        operationType, returnType
                    )
                )
            }
        }
        out.println()
    }


    private fun generateUnaryOperators() {
        for (name in listOf("inc", "dec")) {
            out.printDoc(BasePrimitivesGenerator.incDecOperatorsDoc(name), "    ")
            out.println("    @kotlin.internal.InlineOnly")
            out.println("    public inline operator fun $name(): $className = $className(data.$name())")
            out.println()
        }
    }

    private fun generateRangeTo() {
        val rangeElementType = maxByDomainCapacity(type, UnsignedType.UINT)
        val rangeType = rangeElementType.capitalized + "Range"
        fun convert(name: String) = if (rangeElementType == type) name else "$name.to${rangeElementType.capitalized}()"
        out.println("    /** Creates a range from this value to the specified [other] value. */")
        out.println("    @kotlin.internal.InlineOnly")
        out.println("    public inline operator fun rangeTo(other: $className): $rangeType = $rangeType(${convert("this")}, ${convert("other")})")
        out.println()
    }

    private fun generateRangeUntil() {
        val rangeElementType = maxByDomainCapacity(type, UnsignedType.UINT)
        val rangeType = rangeElementType.capitalized + "Range"
        fun convert(name: String) = if (rangeElementType == type) name else "$name.to${rangeElementType.capitalized}()"
        out.println("    /**")
        out.println("     * Creates a range from this value up to but excluding the specified [other] value.")
        out.println("     *")
        out.println("     * If the [other] value is less than or equal to `this` value, then the returned range is empty.")
        out.println("     */")
        out.println("    @SinceKotlin(\"1.9\")")
        out.println("    @WasExperimental(ExperimentalStdlibApi::class)")
        out.println("    @kotlin.internal.InlineOnly")
        out.println("    public inline operator fun rangeUntil(other: $className): $rangeType = ${convert("this")} until ${convert("other")}")
        out.println()
    }


    private fun generateBitShiftOperators() {

        fun generateShiftOperator(name: String, implementation: String = name) {
            val doc = BasePrimitivesGenerator.shiftOperators[implementation]!!
            val detail = BasePrimitivesGenerator.shiftOperatorsDocDetail(type.asSigned)
            out.printDoc(doc + END_LINE + END_LINE + detail, "    ")
            out.println("    @kotlin.internal.InlineOnly")
            out.println("    public inline infix fun $name(bitCount: Int): $className = $className(data $implementation bitCount)")
            out.println()
        }

        generateShiftOperator("shl")
        generateShiftOperator("shr", "ushr")
    }

    private fun generateBitwiseOperators() {
        for ((name, doc) in BasePrimitivesGenerator.bitwiseOperators) {
            out.println("    /** $doc */")
            out.println("    @kotlin.internal.InlineOnly")
            out.println("    public inline infix fun $name(other: $className): $className = $className(this.data $name other.data)")
        }
        out.println("    /** Inverts the bits in this value. */")
        out.println("    @kotlin.internal.InlineOnly")
        out.println("    public inline fun inv(): $className = $className(data.inv())")
        out.println()
    }

    private fun lsb(count: Int) = "least significant $count bits"
    private fun msb(count: Int) = "most significant $count bits"

    private fun generateMemberConversions() {
        for (otherType in UnsignedType.values()) {
            val signed = otherType.asSigned.capitalized

            out.println("    /**\n     * Converts this [$className] value to [$signed].\n     *")
            when {
                otherType < type -> {
                    out.println("     * If this value is less than or equals to [$signed.MAX_VALUE], the resulting `$signed` value represents")
                    out.println("     * the same numerical value as this `$className`.")
                    out.println("     *")
                    out.println("     * The resulting `$signed` value is represented by the ${lsb(otherType.bitSize)} of this `$className` value.")
                    out.println("     * Note that the resulting `$signed` value may be negative.")
                }
                otherType == type -> {
                    out.println("     * If this value is less than or equals to [$signed.MAX_VALUE], the resulting `$signed` value represents")
                    out.println("     * the same numerical value as this `$className`. Otherwise the result is negative.")
                    out.println("     *")
                    out.println("     * The resulting `$signed` value has the same binary representation as this `$className` value.")
                }
                else -> {
                    out.println("     * The resulting `$signed` value represents the same numerical value as this `$className`.")
                    out.println("     *")
                    out.println("     * The ${lsb(type.bitSize)} of the resulting `$signed` value are the same as the bits of this `$className` value,")
                    out.println("     * whereas the ${msb(otherType.bitSize - type.bitSize)} are filled with zeros.")
                }
            }
            out.println("     */")

            out.println("    @kotlin.internal.InlineOnly")
            out.print("    public inline fun to$signed(): $signed = ")
            out.println(when {
                    type == UnsignedType.UINT && otherType == UnsignedType.ULONG -> "uintToLong(data)"
                    otherType < type -> "data.to$signed()"
                    otherType == type -> "data"
                    else -> "data.to$signed() and ${type.mask}"
                }
            )
        }
        out.println()

        for (otherType in UnsignedType.entries) {
            val name = otherType.capitalized

            if (type == otherType)
                out.println("    /** Returns this value. */")
            else {
                out.println("    /**\n     * Converts this [$className] value to [$name].\n     *")
                when {
                    otherType < type -> {
                        out.println("     * If this value is less than or equals to [$name.MAX_VALUE], the resulting `$name` value represents")
                        out.println("     * the same numerical value as this `$className`.")
                        out.println("     *")
                        out.println("     * The resulting `$name` value is represented by the ${lsb(otherType.bitSize)} of this `$className` value.")
                    }
                    else -> {
                        out.println("     * The resulting `$name` value represents the same numerical value as this `$className`.")
                        out.println("     *")
                        out.println("     * The ${lsb(type.bitSize)} of the resulting `$name` value are the same as the bits of this `$className` value,")
                        out.println("     * whereas the ${msb(otherType.bitSize - type.bitSize)} are filled with zeros.")
                    }
                }
                out.println("     */")
            }

            out.println("    @kotlin.internal.InlineOnly")
            out.print("    public inline fun to$name(): $name = ")
            out.println(
                when {
                    type == UnsignedType.UINT && otherType == UnsignedType.ULONG -> "uintToULong(data)"
                    otherType > type -> "${otherType.capitalized}(data.to${otherType.asSigned.capitalized}() and ${type.mask})"
                    otherType == type -> "this"
                    else -> "data.to${otherType.capitalized}()"
                }
            )
        }
        out.println()
    }

    private fun generateFloatingConversions() {
        for (otherType in PrimitiveType.floatingPoint) {
            val otherName = otherType.capitalized

            out.println("    /**\n     * Converts this [$className] value to [$otherName].\n     *")
            if (type == UnsignedType.ULONG || type == UnsignedType.UINT && otherType == PrimitiveType.FLOAT) {
                out.println("     * The resulting value is the closest `$otherName` to this `$className` value.")
                out.println("     * In case when this `$className` value is exactly between two `$otherName`s,")
                out.println("     * the one with zero at least significant bit of mantissa is selected.")
            } else {
                out.println("     * The resulting `$otherName` value represents the same numerical value as this `$className`.")
            }
            out.println("     */")

            out.println("    @kotlin.internal.InlineOnly")
            out.print("    public inline fun to$otherName(): $otherName = ")
            when (type) {
                UnsignedType.UINT, UnsignedType.ULONG ->
                    out.println(className.lowercase() + "To$otherName(data)")
                else ->
                    out.println("uintTo$otherName(this.toInt())")
            }
        }
        out.println()
    }

    private fun generateExtensionConversions() {
        for (otherType in UnsignedType.entries) {
            val otherSigned = otherType.asSigned.capitalized
            val thisSigned = type.asSigned.capitalized

            out.println("/**\n * Converts this [$otherSigned] value to [$className].\n *")
            when {
                otherType < type -> {
                    out.println(" * If this value is positive, the resulting `$className` value represents the same numerical value as this `$otherSigned`.")
                    out.println(" *")
                    out.println(" * The ${lsb(otherType.bitSize)} of the resulting `$className` value are the same as the bits of this `$otherSigned` value,")
                    out.println(" * whereas the ${msb(type.bitSize - otherType.bitSize)} are filled with the sign bit of this value.")
                }
                otherType == type -> {
                    out.println(" * If this value is positive, the resulting `$className` value represents the same numerical value as this `$otherSigned`.")
                    out.println(" *")
                    out.println(" * The resulting `$className` value has the same binary representation as this `$otherSigned` value.")
                }
                else -> {
                    out.println(" * If this value is positive and less than or equals to [$className.MAX_VALUE], the resulting `$className` value represents")
                    out.println(" * the same numerical value as this `$otherSigned`.")
                    out.println(" *")
                    out.println(" * The resulting `$className` value is represented by the ${lsb(type.bitSize)} of this `$otherSigned` value.")
                }
            }
            out.println(" */")
            out.println("@SinceKotlin(\"1.5\")")
            out.println("@WasExperimental(ExperimentalUnsignedTypes::class)")
            out.println("@kotlin.internal.InlineOnly")
            out.print("public inline fun $otherSigned.to$className(): $className = ")
            out.println(when {
                otherType == type -> "$className(this)"
                else -> "$className(this.to$thisSigned())"
            })
        }

        if (type == UnsignedType.UBYTE || type == UnsignedType.USHORT)
            return // conversion from UByte/UShort to Float/Double is not allowed

        out.println()
        for (otherType in PrimitiveType.floatingPoint) {
            val otherName = otherType.capitalized

            out.println(
                """
                /**
                 * Converts this [$otherName] value to [$className].
                 *
                 * The fractional part, if any, is rounded down towards zero.
                 * Returns zero if this `$otherName` value is negative or `NaN`, [$className.MAX_VALUE] if it's bigger than `$className.MAX_VALUE`.
                 */
                """.trimIndent()
            )
            out.println("@SinceKotlin(\"1.5\")")
            out.println("@WasExperimental(ExperimentalUnsignedTypes::class)")
            out.println("@kotlin.internal.InlineOnly")
            out.print("public inline fun $otherName.to$className(): $className = ")
            out.println("${otherName.lowercase()}To$className(this)")
        }
    }


    private fun generateToStringHashCode() {
        out.print("    public override fun toString(): String = ")
        when (type) {
            UnsignedType.UBYTE, UnsignedType.USHORT -> out.println("toInt().toString()")
            UnsignedType.UINT -> out.println("uintToString(data)")
            UnsignedType.ULONG -> out.println("ulongToString(data)")
        }

        out.println()
    }


    private fun maxByDomainCapacity(type1: UnsignedType, type2: UnsignedType): UnsignedType =
        if (type1.ordinal > type2.ordinal) type1 else type2


    private fun getOperatorReturnType(type1: UnsignedType, type2: UnsignedType): UnsignedType {
        return maxByDomainCapacity(maxByDomainCapacity(type1, type2), UnsignedType.UINT)
    }

}


class UnsignedArrayGenerator(val type: UnsignedType, out: PrintWriter) : BuiltInsSourceGenerator(out) {
    private val elementType = type.capitalized
    private val arrayType = elementType + "Array"
    private val arrayTypeOf = elementType.lowercase() + "ArrayOf"
    private val storageElementType = type.asSigned.capitalized
    private val storageArrayType = storageElementType + "Array"
    override fun generateBody() {
        out.println("import kotlin.jvm.*")
        out.println()

        out.println("@SinceKotlin(\"1.3\")")
        out.println("@ExperimentalUnsignedTypes")
        out.println("@JvmInline")
        out.println("public value class $arrayType")
        out.println("@PublishedApi")
        out.println("internal constructor(@PublishedApi internal val storage: $storageArrayType) : Collection<$elementType> {")
        out.println(
            """
    /** Creates a new array of the specified [size], with all elements initialized to zero. */
    public constructor(size: Int) : this($storageArrayType(size))

    /**
     * Returns the array element at the given [index]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    public operator fun get(index: Int): $elementType = storage[index].to$elementType()

    /**
     * Sets the element at the given [index] to the given [value]. This method can be called using the index operator.
     *
     * If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS
     * where the behavior is unspecified.
     */
    public operator fun set(index: Int, value: $elementType) {
        storage[index] = value.to$storageElementType()
    }

    /** Returns the number of elements in the array. */
    public override val size: Int get() = storage.size

    /** Creates an iterator over the elements of the array. */
    public override operator fun iterator(): kotlin.collections.Iterator<$elementType> = Iterator(storage)

    private class Iterator(private val array: $storageArrayType) : kotlin.collections.Iterator<${elementType}> {
        private var index = 0
        override fun hasNext() = index < array.size
        override fun next() = if (index < array.size) array[index++].to$elementType() else throw NoSuchElementException(index.toString())
    }

    override fun contains(element: $elementType): Boolean {
        // TODO: Eliminate this check after KT-30016 gets fixed.
        // Currently JS BE does not generate special bridge method for this method.
        @Suppress("USELESS_CAST")
        if ((element as Any?) !is $elementType) return false

        return storage.contains(element.to$storageElementType())
    }

    override fun containsAll(elements: Collection<$elementType>): Boolean {
        return (elements as Collection<*>).all { it is $elementType && storage.contains(it.to$storageElementType()) }
    }

    override fun isEmpty(): Boolean = this.storage.size == 0"""
        )

        out.println("}")

        // TODO: Make inline constructor, like in ByteArray
        out.println("""
/**
 * Creates a new array of the specified [size], where each element is calculated by calling the specified
 * [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun $arrayType(size: Int, init: (Int) -> $elementType): $arrayType {
    return $arrayType($storageArrayType(size) { index -> init(index).to$storageElementType() })
}

@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
@kotlin.internal.InlineOnly
public inline fun $arrayTypeOf(vararg elements: $elementType): $arrayType = elements"""
        )
    }
}

class UnsignedRangeGenerator(val type: UnsignedType, out: PrintWriter) : BuiltInsSourceGenerator(out) {
    private val elementType = type.capitalized
    private val signedType = type.asSigned.capitalized
    private val stepType = signedType
    private val stepMinValue = "$stepType.MIN_VALUE"

    override fun getPackage(): String = "kotlin.ranges"

    override fun generateBody() {
        fun hashCodeConversion(name: String, isSigned: Boolean = false) =
            if (type == UnsignedType.ULONG) "($name xor ($name ${if (isSigned) "u" else ""}shr 32))" else name

        out.println(
            """

import kotlin.internal.*

/**
 * A range of values of type `$elementType`.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
public class ${elementType}Range(start: $elementType, endInclusive: $elementType) : ${elementType}Progression(start, endInclusive, 1), ClosedRange<${elementType}>, OpenEndRange<${elementType}> {
    override val start: $elementType get() = first
    override val endInclusive: $elementType get() = last
    
    @Deprecated("Can throw an exception when it's impossible to represent the value with $elementType type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.")
    @SinceKotlin("1.9")
    @WasExperimental(ExperimentalStdlibApi::class)
    override val endExclusive: $elementType get() {
        if (last == $elementType.MAX_VALUE) error("Cannot return the exclusive upper bound of a range that includes MAX_VALUE.")
        return last + 1u
    }

    override fun contains(value: $elementType): Boolean = first <= value && value <= last

    /** 
     * Checks if the range is empty.
     
     * The range is empty if its start value is greater than the end value.
     */
    override fun isEmpty(): Boolean = first > last

    override fun equals(other: Any?): Boolean =
        other is ${elementType}Range && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * ${hashCodeConversion("first")}.toInt() + ${hashCodeConversion("last")}.toInt())

    override fun toString(): String = "${'$'}first..${'$'}last"

    public companion object {
        /** An empty range of values of type $elementType. */
        public val EMPTY: ${elementType}Range = ${elementType}Range($elementType.MAX_VALUE, $elementType.MIN_VALUE)
    }
}

/**
 * A progression of values of type `$elementType`.
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalUnsignedTypes::class)
public open class ${elementType}Progression
internal constructor(
    start: $elementType,
    endInclusive: $elementType,
    step: $stepType
) : Iterable<$elementType> {
    init {
        if (step == 0.to$stepType()) throw kotlin.IllegalArgumentException("Step must be non-zero.")
        if (step == $stepMinValue) throw kotlin.IllegalArgumentException("Step must be greater than $stepMinValue to avoid overflow on negation.")
    }

    /**
     * The first element in the progression.
     */
    public val first: $elementType = start

    /**
     * The last element in the progression.
     */
    public val last: $elementType = getProgressionLastElement(start, endInclusive, step)

    /**
     * The step of the progression.
     */
    public val step: $stepType = step

    final override fun iterator(): Iterator<$elementType> = ${elementType}ProgressionIterator(first, last, step)

    /** 
     * Checks if the progression is empty.
     
     * Progression with a positive step is empty if its first element is greater than the last element.
     * Progression with a negative step is empty if its first element is less than the last element.
     */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is ${elementType}Progression && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * ${hashCodeConversion("first")}.toInt() + ${hashCodeConversion("last")}.toInt()) + ${hashCodeConversion("step", isSigned = true)}.toInt())

    override fun toString(): String = if (step > 0) "${'$'}first..${'$'}last step ${'$'}step" else "${'$'}first downTo ${'$'}last step ${'$'}{-step}"

    public companion object {
        /**
         * Creates ${elementType}Progression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         *
         * [step] must be greater than `$stepMinValue` and not equal to zero.
         */
        public fun fromClosedRange(rangeStart: $elementType, rangeEnd: $elementType, step: $stepType): ${elementType}Progression = ${elementType}Progression(rangeStart, rangeEnd, step)
    }
}


/**
 * An iterator over a progression of values of type `$elementType`.
 * @property step the number by which the value is incremented on each step.
 */
@SinceKotlin("1.3")
private class ${elementType}ProgressionIterator(first: $elementType, last: $elementType, step: $stepType) : Iterator<${elementType}> {
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private val step = step.to$elementType() // use 2-complement math for negative steps
    private var next = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun next(): $elementType {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw kotlin.NoSuchElementException()
            hasNext = false
        } else {
            next += step
        }
        return value
    }
}
"""
        )
    }

}

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.unsigned


import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.convert
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import org.jetbrains.kotlin.generators.builtins.ranges.GeneratePrimitives
import java.io.File
import java.io.PrintWriter


fun generateUnsignedTypes(
    targetDir: File,
    generate: (File, (PrintWriter) -> BuiltInsSourceGenerator) -> Unit
) {
    for (type in UnsignedType.values()) {
        generate(File(targetDir, "kotlin/${type.capitalized}.kt")) { UnsignedTypeGenerator(type, it) }
        generate(File(targetDir, "kotlin/${type.capitalized}Array.kt")) { UnsignedArrayGenerator(type, it) }
    }

    for (type in listOf(UnsignedType.UINT, UnsignedType.ULONG)) {
        generate(File(targetDir, "kotlin/${type.capitalized}Range.kt")) { UnsignedRangeGenerator(type, it) }
    }

    generate(File(targetDir, "kotlin/UIterators.kt"), ::UnsignedIteratorsGenerator)


}

class UnsignedTypeGenerator(val type: UnsignedType, out: PrintWriter) : BuiltInsSourceGenerator(out) {
    val className = type.capitalized
    val storageType = type.asSigned.capitalized

    override fun generateBody() {

        out.println("import kotlin.experimental.*")
        out.println()

        out.println("public inline class $className internal constructor(private val data: $storageType) : Comparable<$className> {")
        out.println()
        out.println("""    companion object {
        /**
         * A constant holding the minimum value an instance of $className can have.
         */
        public /*const*/ val MIN_VALUE: $className = $className(0)

        /**
         * A constant holding the maximum value an instance of $className can have.
         */
        public /*const*/ val MAX_VALUE: $className = $className(-1)
    }""")

        generateCompareTo()

        generateBinaryOperators()
        generateUnaryOperators()
        generateRangeTo()

        if (type == UnsignedType.UINT || type == UnsignedType.ULONG) {
            generateBitShiftOperators()
        }

        generateBitwiseOperators()

        generateMemberConversions()

        out.println("}")
        out.println()


        generateExtensionConversions()
    }


    private fun generateCompareTo() {
        for (otherType in UnsignedType.values()) {
            out.println("""
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */""")
            out.print("    public ")
            if (otherType == type) out.print("override ")
            out.print("operator fun compareTo(other: ${otherType.capitalized}): Int = ")
            if (otherType == type && maxByDomainCapacity(type, UnsignedType.UINT) == type) {
                out.println("${className.toLowerCase()}Compare(this.data, other.data)")
            } else {
                val ctype = maxByDomainCapacity(maxByDomainCapacity(type, otherType), UnsignedType.UINT)
                out.println("${convert("this", type, ctype)}.compareTo(${convert("other", otherType, ctype)})")
            }
        }
        out.println()
    }

    private fun generateBinaryOperators() {
        for ((name, doc) in GeneratePrimitives.binaryOperators) {
            if (name != "mod") {
                generateOperator(name, doc)
            }
        }
    }

    private fun generateOperator(name: String, doc: String) {
        for (otherType in UnsignedType.values()) {
            val returnType = getOperatorReturnType(type, otherType)

            out.println("    /** $doc */")
            out.println("    public operator fun $name(other: ${otherType.capitalized}): ${returnType.capitalized} = TODO()")
        }
        out.println()
    }


    private fun generateUnaryOperators() {
        for ((name, doc) in GeneratePrimitives.unaryOperators) {
            if (name in listOf("unaryPlus", "unaryMinus")) continue // TODO: Decide if unaryPlus and unaryMinus are needed
            out.println("    /** $doc */")
            out.println("    public operator fun $name(): $className = TODO()")
        }
        out.println()
    }

    private fun generateRangeTo() {
        val rangeElementType = maxByDomainCapacity(type, UnsignedType.UINT)
        val rangeType = rangeElementType.capitalized + "Range"
        fun convert(name: String) = if (rangeElementType == type) name else "$name.to${rangeElementType.capitalized}()"
        out.println("    /** Creates a range from this value to the specified [other] value. */")
        out.println("    public operator fun rangeTo(other: $className): $rangeType = $rangeType(${convert("this")}, ${convert("other")})")
        out.println()
    }


    private fun generateBitShiftOperators() {

        fun generateShiftOperator(name: String, implementation: String = name) {
            val doc = GeneratePrimitives.shiftOperators[implementation]!!
            out.println("    /** $doc */")
            out.println("    public infix fun $name(bitCount: Int): $className = $className(data $implementation bitCount)")
        }

        generateShiftOperator("shl")
        generateShiftOperator("shr", "ushr")
    }

    private fun generateBitwiseOperators() {
        for ((name, doc) in GeneratePrimitives.bitwiseOperators) {
            out.println("    /** $doc */")
            out.println("    public infix fun $name(other: $className): $className = $className(this.data $name other.data)")
        }
        out.println("    /** Inverts the bits in this value. */")
        out.println("    public fun inv(): $className = $className(data.inv())")
        out.println()
    }

    private fun generateMemberConversions() {
        for (otherType in UnsignedType.values()) {
            val signed = otherType.asSigned.capitalized
            out.print("    public fun to$signed(): $signed = ")
            out.println(when {
                otherType < type -> "data.to$signed()"
                otherType == type -> "data"
                else -> "data.to$signed() and ${type.mask}"
            })
        }
        out.println()

        for (otherType in UnsignedType.values()) {
            val name = otherType.capitalized
            out.print("    public fun to$name(): $name = ")
            out.println(if (type == otherType) "this" else "data.to${otherType.capitalized}()")
        }
        out.println()
    }

    private fun generateExtensionConversions() {
        for (otherType in UnsignedType.values()) {
            val otherSigned = otherType.asSigned.capitalized
            val thisSigned = type.asSigned.capitalized
            out.print("public fun $otherSigned.to$className(): $className = ")
            out.println(when {
                otherType < type -> "$className(this.to$thisSigned() and ${otherType.mask})"
                otherType == type -> "$className(this)"
                else -> "$className(this.to$thisSigned())"
            })
        }
    }


    private fun maxByDomainCapacity(type1: UnsignedType, type2: UnsignedType): UnsignedType =
        if (type1.ordinal > type2.ordinal) type1 else type2


    private fun getOperatorReturnType(type1: UnsignedType, type2: UnsignedType): UnsignedType {
        return maxByDomainCapacity(maxByDomainCapacity(type1, type2), UnsignedType.UINT)
    }

}


// TODO: reuse generator
class UnsignedIteratorsGenerator(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    override fun getPackage() = "kotlin.collections"
    override fun generateBody() {
        for (type in UnsignedType.values()) {
            val s = type.capitalized
            out.println("/** An iterator over a sequence of values of type `$s`. */")
            out.println("public abstract class ${s}Iterator : Iterator<$s> {")
            // TODO: Sort modifiers
            out.println("    override final fun next() = next$s()")
            out.println()
            out.println("    /** Returns the next value in the sequence without boxing. */")
            out.println("    public abstract fun next$s(): $s")
            out.println("}")
            out.println()
        }
    }
}

class UnsignedArrayGenerator(val type: UnsignedType, out: PrintWriter) : BuiltInsSourceGenerator(out) {
    val elementType = type.capitalized
    val arrayType = elementType + "Array"
    val arrayTypeOf = elementType.toLowerCase() + "ArrayOf"
    val storageElementType = type.asSigned.capitalized
    val storageArrayType = storageElementType + "Array"
    override fun generateBody() {
        out.println("public inline class $arrayType internal constructor(private val storage: $storageArrayType) : Collection<$elementType> {")
        out.println(
            """
    /** Returns the array element at the given [index]. This method can be called using the index operator. */
    public operator fun get(index: Int): $elementType = storage[index].to$elementType()

    /** Sets the element at the given [index] to the given [value]. This method can be called using the index operator. */
    public operator fun set(index: Int, value: $elementType) {
        storage[index] = value.to$storageElementType()
    }

    /** Returns the number of elements in the array. */
    public override val size: Int get() = storage.size

    /** Creates an iterator over the elements of the array. */
    public override operator fun iterator(): ${elementType}Iterator = Iterator(storage)

    private class Iterator(private val array: $storageArrayType) : ${elementType}Iterator() {
        private var index = 0
        override fun hasNext() = index < array.size
        override fun next$elementType() = if (index < array.size) array[index++].to$elementType() else throw NoSuchElementException(index.toString())
    }

    override fun contains(element: $elementType): Boolean = storage.contains(element.to$storageElementType())

    override fun containsAll(elements: Collection<$elementType>): Boolean = elements.all { storage.contains(it.to$storageElementType()) }

    override fun isEmpty(): Boolean = this.storage.size == 0"""
        )

        out.println("}")

        out.println("""
public /*inline*/ fun $arrayType(size: Int, init: (Int) -> $elementType): $arrayType {
    return $arrayType($storageArrayType(size) { index -> init(index).to$storageElementType() })
}

public fun $arrayTypeOf(vararg elements: $elementType): $arrayType {
    return $arrayType(elements.size) { index -> elements[index] }
}"""
        )
    }
}

class UnsignedRangeGenerator(val type: UnsignedType, out: PrintWriter) : BuiltInsSourceGenerator(out) {
    val elementType = type.capitalized
    val signedType = type.asSigned.capitalized
    val stepType = signedType

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
public class ${elementType}Range(start: $elementType, endInclusive: $elementType) : ${elementType}Progression(start, endInclusive, 1), ClosedRange<${elementType}> {
    override val start: $elementType get() = first
    override val endInclusive: $elementType get() = last

    override fun contains(value: $elementType): Boolean = first <= value && value <= last

    override fun isEmpty(): Boolean = first > last

    override fun equals(other: Any?): Boolean =
        other is ${elementType}Range && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * ${hashCodeConversion("first")}.toInt() + ${hashCodeConversion("last")}.toInt())

    override fun toString(): String = "${'$'}first..${'$'}last"

    companion object {
        /** An empty range of values of type $elementType. */
        public val EMPTY: ${elementType}Range = ${elementType}Range($elementType.MAX_VALUE, $elementType.MIN_VALUE)
    }
}

/**
 * A progression of values of type `$elementType`.
 */
public open class ${elementType}Progression
internal constructor(
    start: $elementType,
    endInclusive: $elementType,
    step: $stepType
) : Iterable<$elementType> {
    init {
        if (step == 0.to$stepType()) throw kotlin.IllegalArgumentException("Step must be non-zero")
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

    override fun iterator(): ${elementType}Iterator = ${elementType}ProgressionIterator(first, last, step)

    /** Checks if the progression is empty. */
    public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

    override fun equals(other: Any?): Boolean =
        other is ${elementType}Progression && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * ${hashCodeConversion("first")}.toInt() + ${hashCodeConversion("last")}.toInt()) + ${hashCodeConversion("step", isSigned = true)}.toInt())

    override fun toString(): String = if (step > 0) "${'$'}first..${'$'}last step ${'$'}step" else "${'$'}first downTo ${'$'}last step ${'$'}{-step}"

    companion object {
        /**
         * Creates ${elementType}Progression within the specified bounds of a closed range.

         * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
         * In order to go backwards the [step] must be negative.
         */
        public fun fromClosedRange(rangeStart: $elementType, rangeEnd: $elementType, step: $stepType): ${elementType}Progression = ${elementType}Progression(rangeStart, rangeEnd, step)
    }
}


/**
 * An iterator over a progression of values of type `$elementType`.
 * @property step the number by which the value is incremented on each step.
 */
private class ${elementType}ProgressionIterator(first: $elementType, last: $elementType, step: $stepType) : ${elementType}Iterator() {
    private val finalElement = last
    private var hasNext: Boolean = if (step > 0) first <= last else first >= last
    private val step = step.to$elementType() // use 2-complement math for negative steps
    private var next = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun next$elementType(): $elementType {
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

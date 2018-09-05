/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.ranges

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import java.io.PrintWriter

class GeneratePrimitives(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    companion object {
        internal val binaryOperators: Map<String, String> = mapOf(
            "plus" to "Adds the other value to this value.",
            "minus" to "Subtracts the other value from this value.",
            "times" to "Multiplies this value by the other value.",
            "div" to "Divides this value by the other value.",
            "mod" to "Calculates the remainder of dividing this value by the other value.",
            "rem" to "Calculates the remainder of dividing this value by the other value."
        )
        internal val unaryOperators: Map<String, String> = mapOf(
            "inc" to "Increments this value.",
            "dec" to "Decrements this value.",
            "unaryPlus" to "Returns this value.",
            "unaryMinus" to "Returns the negative of this value.")
        internal val shiftOperators: Map<String, String> = mapOf(
            "shl" to "Shifts this value left by the [bitCount] number of bits.",
            "shr" to "Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with copies of the sign bit.",
            "ushr" to "Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros.")
        internal val bitwiseOperators: Map<String, String> = mapOf(
            "and" to "Performs a bitwise AND operation between the two values.",
            "or" to "Performs a bitwise OR operation between the two values.",
            "xor" to "Performs a bitwise XOR operation between the two values.")
    }
    private val typeDescriptions: Map<PrimitiveType, String> = mapOf(
            PrimitiveType.DOUBLE to "double-precision 64-bit IEEE 754 floating point number",
            PrimitiveType.FLOAT to "single-precision 32-bit IEEE 754 floating point number",
            PrimitiveType.LONG to "64-bit signed integer",
            PrimitiveType.INT to "32-bit signed integer",
            PrimitiveType.SHORT to "16-bit signed integer",
            PrimitiveType.BYTE to "8-bit signed integer",
            PrimitiveType.CHAR to "16-bit Unicode character"
    )

    private fun primitiveConstants(type: PrimitiveType): List<Any> = when (type) {
        PrimitiveType.INT -> listOf(java.lang.Integer.MIN_VALUE, java.lang.Integer.MAX_VALUE)
        PrimitiveType.BYTE -> listOf(java.lang.Byte.MIN_VALUE, java.lang.Byte.MAX_VALUE)
        PrimitiveType.SHORT -> listOf(java.lang.Short.MIN_VALUE, java.lang.Short.MAX_VALUE)
        PrimitiveType.LONG -> listOf((java.lang.Long.MIN_VALUE + 1).toString() + "L - 1L", java.lang.Long.MAX_VALUE.toString() + "L")
//        PrimitiveType.DOUBLE -> listOf(java.lang.Double.MIN_VALUE, java.lang.Double.MAX_VALUE, "1.0/0.0", "-1.0/0.0", "0.0/0.0")
//        PrimitiveType.FLOAT -> listOf(java.lang.Float.MIN_VALUE, java.lang.Float.MAX_VALUE, "1.0F/0.0F", "-1.0F/0.0F", "0.0F/0.0F").map { it as? String ?: "${it}F" }
        else -> throw IllegalArgumentException("type: $type")
    }

    override fun generateBody() {
        for (kind in PrimitiveType.onlyNumeric) {
            val className = kind.capitalized
            generateDoc(kind)
            out.println("public class $className private constructor() : Number(), Comparable<$className> {")

            out.print("    companion object {")
            if (kind == PrimitiveType.FLOAT || kind == PrimitiveType.DOUBLE) {
                //val (minValue, maxValue, posInf, negInf, nan) = primitiveConstants(kind)
                out.println("""
        /**
         * A constant holding the smallest *positive* nonzero value of $className.
         */
        public val MIN_VALUE: $className

        /**
         * A constant holding the largest positive finite value of $className.
         */
        public val MAX_VALUE: $className

        /**
         * A constant holding the positive infinity value of $className.
         */
        public val POSITIVE_INFINITY: $className

        /**
         * A constant holding the negative infinity value of $className.
         */
        public val NEGATIVE_INFINITY: $className

        /**
         * A constant holding the "not a number" value of $className.
         */
        public val NaN: $className""")
            }
            if (kind == PrimitiveType.INT || kind == PrimitiveType.LONG || kind == PrimitiveType.SHORT || kind == PrimitiveType.BYTE) {
                val (minValue, maxValue) = primitiveConstants(kind)
                out.println("""
        /**
         * A constant holding the minimum value an instance of $className can have.
         */
        public const val MIN_VALUE: $className = $minValue

        /**
         * A constant holding the maximum value an instance of $className can have.
         */
        public const val MAX_VALUE: $className = $maxValue""")
            }
            if (kind.byteSize != null) {
                out.println("""
        /**
         * The number of bytes used to represent an instance of $className in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BYTES: Int = ${kind.byteSize}

        /**
         * The number of bits used to represent an instance of $className in a binary form.
         */
        @SinceKotlin("1.3")
        public const val SIZE_BITS: Int = ${kind.byteSize * 8}""")
            }
            out.println("""    }""")

            generateCompareTo(kind)

            generateBinaryOperators(kind)
            generateUnaryOperators(kind)
            generateRangeTo(kind)

            if (kind == PrimitiveType.INT || kind == PrimitiveType.LONG) {
                generateBitShiftOperators(className)
            }
            if (kind == PrimitiveType.INT || kind == PrimitiveType.LONG /* || kind == PrimitiveType.BYTE || kind == PrimitiveType.SHORT */) {
                generateBitwiseOperators(className, since = if (kind == PrimitiveType.BYTE || kind == PrimitiveType.SHORT) "1.1" else null)
            }

            generateConversions()

            out.println("}\n")
        }
    }

    private fun generateDoc(kind: PrimitiveType) {
        out.println("/**")
        out.println(" * Represents a ${typeDescriptions[kind]}.")
        out.println(" * On the JVM, non-nullable values of this type are represented as values of the primitive type `${kind.name.toLowerCase()}`.")
        out.println(" */")
    }

    private fun generateCompareTo(thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            out.println("""
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */""")
            out.print("    public ")
            if (otherKind == thisKind) out.print("override ")
            out.println("operator fun compareTo(other: ${otherKind.capitalized}): Int")
        }
        out.println()
    }

    private fun generateBinaryOperators(thisKind: PrimitiveType) {
        for ((name, doc) in binaryOperators) {
            generateOperator(name, doc, thisKind)
        }
    }

    private fun generateOperator(name: String, doc: String, thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            val returnType = getOperatorReturnType(thisKind, otherKind)

            out.println("    /** $doc */")
            when (name) {
                "rem" ->
                    out.println("    @SinceKotlin(\"1.1\")")

                "mod" ->
                    out.println("    @Deprecated(\"Use rem(other) instead\", ReplaceWith(\"rem(other)\"), DeprecationLevel.WARNING)")
            }
            out.println("    public operator fun $name(other: ${otherKind.capitalized}): ${returnType.capitalized}")
        }
        out.println()
    }

    private fun generateRangeTo(thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            val returnType =
                maxByDomainCapacity(thisKind, otherKind)
                .let { if (it == PrimitiveType.CHAR) it else maxByDomainCapacity(it, PrimitiveType.INT) }
            if (returnType == PrimitiveType.DOUBLE || returnType == PrimitiveType.FLOAT)
                continue
            out.println("     /** Creates a range from this value to the specified [other] value. */")
            out.println("    public operator fun rangeTo(other: ${otherKind.capitalized}): ${returnType.capitalized}Range")
        }
        out.println()

    }

    private fun generateUnaryOperators(kind: PrimitiveType) {
        for ((name, doc) in unaryOperators) {
            val returnType = if (kind in listOf(PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR) &&
                                 name in listOf("unaryPlus", "unaryMinus")) "Int" else kind.capitalized
            out.println("    /** $doc */")
            out.println("    public operator fun $name(): $returnType")
        }
        out.println()
    }

    private fun generateBitShiftOperators(className: String) {
        for ((name, doc) in shiftOperators) {
            out.println("    /** $doc */")
            out.println("    public infix fun $name(bitCount: Int): $className")
        }
    }
    private fun generateBitwiseOperators(className: String, since: String?) {
        for ((name, doc) in bitwiseOperators) {
            out.println("    /** $doc */")
            since?.let { out.println("    @SinceKotlin(\"$it\")") }
            out.println("    public infix fun $name(other: $className): $className")
        }
        out.println("    /** Inverts the bits in this value. */")
        since?.let { out.println("    @SinceKotlin(\"$it\")") }
        out.println("    public fun inv(): $className")
        out.println()
    }

    private fun generateConversions() {
        for (otherKind in PrimitiveType.exceptBoolean) {
            val name = otherKind.capitalized
            out.println("    public override fun to$name(): $name")
        }
    }

    private fun maxByDomainCapacity(type1: PrimitiveType, type2: PrimitiveType): PrimitiveType
            = if (type1.ordinal > type2.ordinal) type1 else type2

    private fun getOperatorReturnType(kind1: PrimitiveType, kind2: PrimitiveType): PrimitiveType {
        require(kind1 != PrimitiveType.BOOLEAN) { "kind1 must not be BOOLEAN" }
        require(kind2 != PrimitiveType.BOOLEAN) { "kind2 must not be BOOLEAN" }
        return maxByDomainCapacity(maxByDomainCapacity(kind1, kind2), PrimitiveType.INT)
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.convert
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import org.jetbrains.kotlin.generators.builtins.printDoc
import java.io.PrintWriter

class GeneratePrimitives(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    companion object {
        internal val binaryOperators: List<String> = listOf(
            "plus",
            "minus",
            "times",
            "div",
            "rem",
        )
        internal val unaryPlusMinusOperators: Map<String, String> = mapOf(
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


        internal fun shiftOperatorsDocDetail(kind: PrimitiveType): String {
            val bitsUsed = when (kind) {
                PrimitiveType.INT -> "five"
                PrimitiveType.LONG -> "six"
                else -> throw IllegalArgumentException("Bit shift operation is not implemented for $kind")
            }
            return """ 
                * Note that only the $bitsUsed lowest-order bits of the [bitCount] are used as the shift distance.
                * The shift distance actually used is therefore always in the range `0..${kind.bitSize - 1}`.
                """
        }

        internal fun incDecOperatorsDoc(name: String): String {
            val diff = if (name == "inc") "incremented" else "decremented"

            return """
                /**
                 * Returns this value $diff by one.
                 *
                 * @sample samples.misc.Builtins.$name
                 */
                """
        }

        internal fun binaryOperatorDoc(operator: String, operand1: PrimitiveType, operand2: PrimitiveType): String = when (operator) {
            "plus" -> "Adds the other value to this value."
            "minus" -> "Subtracts the other value from this value."
            "times" -> "Multiplies this value by the other value."
            "div" -> {
                if (operand1.isIntegral && operand2.isIntegral)
                    "Divides this value by the other value, truncating the result to an integer that is closer to zero."
                else
                    "Divides this value by the other value."
            }
            "floorDiv" ->
                "Divides this value by the other value, flooring the result to an integer that is closer to negative infinity."
            "rem" -> {
                """
                Calculates the remainder of truncating division of this value (dividend) by the other value (divisor).
                
                The result is either zero or has the same sign as the _dividend_ and has the absolute value less than the absolute value of the divisor.
                """.trimIndent()
            }
            "mod" -> {
                """
                Calculates the remainder of flooring division of this value (dividend) by the other value (divisor).

                The result is either zero or has the same sign as the _divisor_ and has the absolute value less than the absolute value of the divisor.
                """.trimIndent() + if (operand1.isFloatingPoint)
                    "\n\n" + "If the result cannot be represented exactly, it is rounded to the nearest representable number. In this case the absolute value of the result can be less than or _equal to_ the absolute value of the divisor."
                else ""
            }
            else -> error("No documentation for operator $operator")
        }
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
        PrimitiveType.DOUBLE -> listOf(java.lang.Double.MIN_VALUE, java.lang.Double.MAX_VALUE, "1.0/0.0", "-1.0/0.0", "-(0.0/0.0)")
        PrimitiveType.FLOAT -> listOf(java.lang.Float.MIN_VALUE, java.lang.Float.MAX_VALUE, "1.0F/0.0F", "-1.0F/0.0F", "-(0.0F/0.0F)").map { it as? String ?: "${it}F" }
        else -> throw IllegalArgumentException("type: $type")
    }

    override fun generateBody() {
        for (kind in PrimitiveType.onlyNumeric) {
            val className = kind.capitalized
            generateDoc(kind)
            out.println("public class $className private constructor() : Number(), Comparable<$className> {")

            out.print("    companion object {")
            if (kind == PrimitiveType.FLOAT || kind == PrimitiveType.DOUBLE) {
                val (minValue, maxValue, posInf, negInf, nan) = primitiveConstants(kind)
                out.println("""
        /**
         * A constant holding the smallest *positive* nonzero value of $className.
         */
        public const val MIN_VALUE: $className = $minValue

        /**
         * A constant holding the largest positive finite value of $className.
         */
        public const val MAX_VALUE: $className = $maxValue

        /**
         * A constant holding the positive infinity value of $className.
         */
        public const val POSITIVE_INFINITY: $className = $posInf

        /**
         * A constant holding the negative infinity value of $className.
         */
        public const val NEGATIVE_INFINITY: $className = $negInf

        /**
         * A constant holding the "not a number" value of $className.
         */
        public const val NaN: $className = $nan""")
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
            if (kind.isIntegral || kind.isFloatingPoint) {
                val sizeSince = if (kind.isFloatingPoint) "1.4" else "1.3"
                out.println("""
        /**
         * The number of bytes used to represent an instance of $className in a binary form.
         */
        @SinceKotlin("$sizeSince")
        public const val SIZE_BYTES: Int = ${kind.byteSize}

        /**
         * The number of bits used to represent an instance of $className in a binary form.
         */
        @SinceKotlin("$sizeSince")
        public const val SIZE_BITS: Int = ${kind.bitSize}""")
            }
            out.println("""    }""")

            generateCompareTo(kind)

            generateBinaryOperators(kind)
            generateUnaryOperators(kind)
            generateRangeTo(kind)
            generateRangeUntil(kind)

            if (kind == PrimitiveType.INT || kind == PrimitiveType.LONG) {
                generateBitShiftOperators(kind)
            }
            if (kind == PrimitiveType.INT || kind == PrimitiveType.LONG /* || kind == PrimitiveType.BYTE || kind == PrimitiveType.SHORT */) {
                generateBitwiseOperators(className, since = if (kind == PrimitiveType.BYTE || kind == PrimitiveType.SHORT) "1.1" else null)
            }

            generateConversions(kind)
            generateEquals()
            generateToString()

            out.println("}\n")
        }
    }

    private fun generateDoc(kind: PrimitiveType) {
        out.println("/**")
        out.println(" * Represents a ${typeDescriptions[kind]}.")
        out.println(" * On the JVM, non-nullable values of this type are represented as values of the primitive type `${kind.name.lowercase()}`.")
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
            out.println("    @kotlin.internal.IntrinsicConstEvaluation")
            out.print("    public ")
            if (otherKind == thisKind) out.print("override ")
            out.println("operator fun compareTo(other: ${otherKind.capitalized}): Int")
        }
        out.println()
    }

    private fun generateBinaryOperators(thisKind: PrimitiveType) {
        for (name in binaryOperators) {
            generateOperator(name, thisKind)
        }
    }

    private fun generateOperator(name: String, thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            val returnType = getOperatorReturnType(thisKind, otherKind)

            out.printDoc(binaryOperatorDoc(name, thisKind, otherKind), "    ")
            when (name) {
                "rem" ->
                    out.println("    @SinceKotlin(\"1.1\")")
            }
            out.println("    @kotlin.internal.IntrinsicConstEvaluation")
            out.println("    public operator fun $name(other: ${otherKind.capitalized}): ${returnType.capitalized}")
        }
        out.println()
    }

    private fun generateRangeTo(thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            val returnType = maxByDomainCapacity(maxByDomainCapacity(thisKind, otherKind), PrimitiveType.INT)
            if (returnType == PrimitiveType.DOUBLE || returnType == PrimitiveType.FLOAT)
                continue
            out.println("     /** Creates a range from this value to the specified [other] value. */")
            out.println("    public operator fun rangeTo(other: ${otherKind.capitalized}): ${returnType.capitalized}Range")
        }
        out.println()

    }

    private fun generateRangeUntil(thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            val returnType = maxByDomainCapacity(maxByDomainCapacity(thisKind, otherKind), PrimitiveType.INT)
            if (returnType == PrimitiveType.DOUBLE || returnType == PrimitiveType.FLOAT)
                continue
            out.println("    /**")
            out.println("     * Creates a range from this value up to but excluding the specified [other] value.")
            out.println("     *")
            out.println("     * If the [other] value is less than or equal to `this` value, then the returned range is empty.")
            out.println("     */")
            out.println("    @SinceKotlin(\"1.7\")")
            out.println("    @ExperimentalStdlibApi")
            out.println("    public operator fun rangeUntil(other: ${otherKind.capitalized}): ${returnType.capitalized}Range")
            out.println()
        }
    }

    private fun generateUnaryOperators(kind: PrimitiveType) {
        for (name in listOf("inc", "dec")) {
            out.println(incDecOperatorsDoc(name).replaceIndent("    "))
            out.println("    public operator fun $name(): ${kind.capitalized}")
            out.println()
        }

        for ((name, doc) in unaryPlusMinusOperators) {
            val returnType = if (kind in listOf(PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR)) "Int" else kind.capitalized
            out.println("    /** $doc */")
            out.println("    @kotlin.internal.IntrinsicConstEvaluation")
            out.println("    public operator fun $name(): $returnType")
        }
        out.println()
    }

    private fun generateBitShiftOperators(kind: PrimitiveType) {
        val className = kind.capitalized
        val detail = shiftOperatorsDocDetail(kind)
        for ((name, doc) in shiftOperators) {
            out.println("    /**")
            out.println("     * $doc")
            out.println("     *")
            out.println(detail.replaceIndent("     "))
            out.println("     */")
            out.println("    @kotlin.internal.IntrinsicConstEvaluation")
            out.println("    public infix fun $name(bitCount: Int): $className")
            out.println()
        }
    }
    private fun generateBitwiseOperators(className: String, since: String?) {
        for ((name, doc) in bitwiseOperators) {
            out.println("    /** $doc */")
            since?.let { out.println("    @SinceKotlin(\"$it\")") }
            out.println("    @kotlin.internal.IntrinsicConstEvaluation")
            out.println("    public infix fun $name(other: $className): $className")
        }
        out.println("    /** Inverts the bits in this value. */")
        since?.let { out.println("    @SinceKotlin(\"$it\")") }
        out.println("    @kotlin.internal.IntrinsicConstEvaluation")
        out.println("    public fun inv(): $className")
        out.println()
    }


    private fun compareByDomainCapacity(type1: PrimitiveType, type2: PrimitiveType): Int =
        if (type1.isIntegral && type2.isIntegral) type1.byteSize - type2.byteSize else type1.ordinal - type2.ordinal

    private fun docForConversionFromFloatingToIntegral(fromFloating: PrimitiveType, toIntegral: PrimitiveType): String {
        require(fromFloating.isFloatingPoint)
        require(toIntegral.isIntegral)

        val thisName = fromFloating.capitalized
        val otherName = toIntegral.capitalized

        return if (compareByDomainCapacity(toIntegral, PrimitiveType.INT) < 0) {
            """
             * The resulting `$otherName` value is equal to `this.toInt().to$otherName()`.
             */
            """
        } else {
            """
             * The fractional part, if any, is rounded down towards zero.
             * Returns zero if this `$thisName` value is `NaN`, [$otherName.MIN_VALUE] if it's less than `$otherName.MIN_VALUE`,
             * [$otherName.MAX_VALUE] if it's bigger than `$otherName.MAX_VALUE`.
             */
            """
        }
    }

    private fun docForConversionFromFloatingToFloating(fromFloating: PrimitiveType, toFloating: PrimitiveType): String {
        require(fromFloating.isFloatingPoint)
        require(toFloating.isFloatingPoint)

        val thisName = fromFloating.capitalized
        val otherName = toFloating.capitalized

        return if (compareByDomainCapacity(toFloating, fromFloating) < 0) {
            """
             * The resulting value is the closest `$otherName` to this `$thisName` value.
             * In case when this `$thisName` value is exactly between two `$otherName`s,
             * the one with zero at least significant bit of mantissa is selected.
             */
            """
        } else {
            """
             * The resulting `$otherName` value represents the same numerical value as this `$thisName`.
             */
            """
        }
    }

    private fun docForConversionFromIntegralToIntegral(fromIntegral: PrimitiveType, toIntegral: PrimitiveType): String {
        require(fromIntegral.isIntegral)
        require(toIntegral.isIntegral)

        val thisName = fromIntegral.capitalized
        val otherName = toIntegral.capitalized

        return if (toIntegral == PrimitiveType.CHAR) {
            if (fromIntegral == PrimitiveType.SHORT) {
                """
                * The resulting `Char` code is equal to this value reinterpreted as an unsigned number,
                * i.e. it has the same binary representation as this `Short`.
                */
                """
            } else if (fromIntegral == PrimitiveType.BYTE) {
                """
                * If this value is non-negative, the resulting `Char` code is equal to this value.
                *
                * The least significant 8 bits of the resulting `Char` code are the same as the bits of this `Byte` value,
                * whereas the most significant 8 bits are filled with the sign bit of this value.
                */
                """
            } else {
                """
                * If this value is in the range of `Char` codes `Char.MIN_VALUE..Char.MAX_VALUE`,
                * the resulting `Char` code is equal to this value.
                *
                * The resulting `Char` code is represented by the least significant 16 bits of this `$thisName` value.
                */
                """
            }
        } else if (compareByDomainCapacity(toIntegral, fromIntegral) < 0) {
            """
             * If this value is in [$otherName.MIN_VALUE]..[$otherName.MAX_VALUE], the resulting `$otherName` value represents
             * the same numerical value as this `$thisName`.
             *
             * The resulting `$otherName` value is represented by the least significant ${toIntegral.bitSize} bits of this `$thisName` value.
             */
            """
        } else {
            """
             * The resulting `$otherName` value represents the same numerical value as this `$thisName`.
             *
             * The least significant ${fromIntegral.bitSize} bits of the resulting `$otherName` value are the same as the bits of this `$thisName` value,
             * whereas the most significant ${toIntegral.bitSize - fromIntegral.bitSize} bits are filled with the sign bit of this value.
             */
            """
        }
    }

    private fun docForConversionFromIntegralToFloating(fromIntegral: PrimitiveType, toFloating: PrimitiveType): String {
        require(fromIntegral.isIntegral)
        require(toFloating.isFloatingPoint)

        val thisName = fromIntegral.capitalized
        val otherName = toFloating.capitalized

        return if (fromIntegral == PrimitiveType.LONG || fromIntegral == PrimitiveType.INT && toFloating == PrimitiveType.FLOAT) {
            """
             * The resulting value is the closest `$otherName` to this `$thisName` value.
             * In case when this `$thisName` value is exactly between two `$otherName`s,
             * the one with zero at least significant bit of mantissa is selected.
             */
            """
        } else {
            """
             * The resulting `$otherName` value represents the same numerical value as this `$thisName`.
             */
            """
        }
    }

    private fun generateConversions(kind: PrimitiveType) {
        fun isFpToIntConversionDeprecated(otherKind: PrimitiveType): Boolean {
            return kind in PrimitiveType.floatingPoint && otherKind in listOf(PrimitiveType.BYTE, PrimitiveType.SHORT)
        }

        val thisName = kind.capitalized
        for (otherKind in PrimitiveType.exceptBoolean) {
            val otherName = otherKind.capitalized
            val doc = if (kind == otherKind) {
                "    /** Returns this value. */"
            } else {
                val detail = if (kind in PrimitiveType.integral) {
                    if (otherKind.isIntegral) {
                        docForConversionFromIntegralToIntegral(kind, otherKind)
                    } else {
                        docForConversionFromIntegralToFloating(kind, otherKind)
                    }
                } else {
                    if (otherKind.isIntegral) {
                        docForConversionFromFloatingToIntegral(kind, otherKind)
                    } else {
                        docForConversionFromFloatingToFloating(kind, otherKind)
                    }
                }

                "    /**\n     * Converts this [$thisName] value to [$otherName].\n     *\n" + detail.replaceIndent("     ")
            }
            out.println(doc)

            if (isFpToIntConversionDeprecated(otherKind)) {
                out.println("    @Deprecated(\"Unclear conversion. To achieve the same result convert to Int explicitly and then to $otherName.\", ReplaceWith(\"toInt().to$otherName()\"))")
                out.println("    @DeprecatedSinceKotlin(warningSince = \"1.3\", errorSince = \"1.5\")")
            }
            if (otherKind == PrimitiveType.CHAR) {
                if (kind == PrimitiveType.INT) {
                    out.println("    @Suppress(\"OVERRIDE_DEPRECATION\")")
                } else {
                    out.println("    @Deprecated(\"Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.\", ReplaceWith(\"this.toInt().toChar()\"))")
                    out.println("    @DeprecatedSinceKotlin(warningSince = \"1.5\", errorSince = \"2.3\")")
                }
            }

            out.println("    @kotlin.internal.IntrinsicConstEvaluation")
            out.println("    public override fun to$otherName(): $otherName")
        }
        out.println()
    }

    private fun generateEquals() {
        out.println("    @kotlin.internal.IntrinsicConstEvaluation")
        out.println("    public override fun equals(other: Any?): Boolean")
        out.println()
    }

    private fun generateToString() {
        out.println("    @kotlin.internal.IntrinsicConstEvaluation")
        out.println("    public override fun toString(): String")
    }
}

class GenerateFloorDivMod(out: PrintWriter) : BuiltInsSourceGenerator(out) {

    override fun getMultifileClassName() = "NumbersKt"
    override fun generateBody() {
        out.println("import kotlin.math.sign")
        out.println()

        val integerTypes = PrimitiveType.integral intersect PrimitiveType.onlyNumeric
        for (thisType in integerTypes) {
            for (otherType in integerTypes) {
                generateFloorDiv(thisType, otherType)
                generateMod(thisType, otherType)
            }
        }

        val fpTypes = PrimitiveType.floatingPoint
        for (thisType in fpTypes) {
            for (otherType in fpTypes) {
                generateFpMod(thisType, otherType)
            }
        }

    }


    private fun generateFloorDiv(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        val returnType = getOperatorReturnType(thisKind, otherKind)
        val returnTypeName = returnType.capitalized
        out.printDoc(GeneratePrimitives.binaryOperatorDoc("floorDiv", thisKind, otherKind), "")
        out.println("""@SinceKotlin("1.5")""")
        out.println("@kotlin.internal.InlineOnly")
        out.println("@kotlin.internal.IntrinsicConstEvaluation")
        val declaration = "public inline fun ${thisKind.capitalized}.floorDiv(other: ${otherKind.capitalized}): $returnTypeName"
        if (thisKind == otherKind && thisKind >= PrimitiveType.INT) {
            out.println(
                """
                    $declaration {
                        var q = this / other
                        if (this xor other < 0 && q * other != this) q-- 
                        return q
                    }
                """.trimIndent()
            )
        } else {
            out.println("$declaration = ")
            out.println("    ${
                convert("this", thisKind, returnType)}.floorDiv(${convert("other", otherKind, returnType)})")
        }
        out.println()
    }

    private fun generateMod(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        val operationType = getOperatorReturnType(thisKind, otherKind)
        val returnType = otherKind
        out.printDoc(GeneratePrimitives.binaryOperatorDoc("mod", thisKind, otherKind),"")
        out.println("""@SinceKotlin("1.5")""")
        out.println("@kotlin.internal.InlineOnly")
        out.println("@kotlin.internal.IntrinsicConstEvaluation")
        val declaration = "public inline fun ${thisKind.capitalized}.mod(other: ${otherKind.capitalized}): ${returnType.capitalized}"
        if (thisKind == otherKind && thisKind >= PrimitiveType.INT) {
            out.println(
                """
                    $declaration {
                        val r = this % other
                        return r + (other and (((r xor other) and (r or -r)) shr ${operationType.bitSize - 1}))
                    }
                """.trimIndent()
            )
        } else {
            out.println("$declaration = ")
            out.println("    " + convert(
                "${convert("this", thisKind, operationType)}.mod(${convert("other", otherKind, operationType)})",
                operationType, returnType
            ))
        }
        out.println()
    }

    private fun generateFpMod(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        val operationType = getOperatorReturnType(thisKind, otherKind)
        out.printDoc(GeneratePrimitives.binaryOperatorDoc("mod", thisKind, otherKind), "")
        out.println("""@SinceKotlin("1.5")""")
        out.println("@kotlin.internal.InlineOnly")
        out.println("@kotlin.internal.IntrinsicConstEvaluation")
        val declaration = "public inline fun ${thisKind.capitalized}.mod(other: ${otherKind.capitalized}): ${operationType.capitalized}"
        if (thisKind == otherKind && thisKind >= PrimitiveType.INT) {
            out.println(
                """
                    $declaration {
                        val r = this % other
                        return if (r != ${convert("0.0", PrimitiveType.DOUBLE, operationType)} && r.sign != other.sign) r + other else r
                    }
                """.trimIndent()
            )
        } else {
            out.println("$declaration = ")
            out.println("    ${convert("this", thisKind, operationType)}.mod(${convert("other", otherKind, operationType)})")
        }
        out.println()
    }

}


private fun maxByDomainCapacity(type1: PrimitiveType, type2: PrimitiveType): PrimitiveType
        = if (type1.ordinal > type2.ordinal) type1 else type2

private fun getOperatorReturnType(kind1: PrimitiveType, kind2: PrimitiveType): PrimitiveType {
    require(kind1 != PrimitiveType.BOOLEAN) { "kind1 must not be BOOLEAN" }
    require(kind2 != PrimitiveType.BOOLEAN) { "kind2 must not be BOOLEAN" }
    return maxByDomainCapacity(maxByDomainCapacity(kind1, kind2), PrimitiveType.INT)
}


/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsGenerator
import java.io.PrintWriter

abstract class BasePrimitivesGenerator(private val writer: PrintWriter) : BuiltInsGenerator {
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
            "unaryMinus" to "Returns the negative of this value."
        )

        internal val shiftOperators: Map<String, String> = mapOf(
            "shl" to "Shifts this value left by the [bitCount] number of bits.",
            "shr" to "Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with copies of the sign bit.",
            "ushr" to "Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros."
        )

        internal val bitwiseOperators: Map<String, String> = mapOf(
            "and" to "Performs a bitwise AND operation between the two values.",
            "or" to "Performs a bitwise OR operation between the two values.",
            "xor" to "Performs a bitwise XOR operation between the two values."
        )

        internal fun shiftOperatorsDocDetail(kind: PrimitiveType): String {
            val bitsUsed = when (kind) {
                PrimitiveType.INT -> "five"
                PrimitiveType.LONG -> "six"
                else -> throw IllegalArgumentException("Bit shift operation is not implemented for $kind")
            }
            return """ 
                Note that only the $bitsUsed lowest-order bits of the [bitCount] are used as the shift distance.
                The shift distance actually used is therefore always in the range `0..${kind.bitSize - 1}`.
                """.trimIndent()
        }

        internal fun incDecOperatorsDoc(name: String): String {
            val diff = if (name == "inc") "incremented" else "decremented"

            return """
                Returns this value $diff by one.

                @sample samples.misc.Builtins.$name
            """.trimIndent()
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

        private fun compareByDomainCapacity(type1: PrimitiveType, type2: PrimitiveType): Int {
            return if (type1.isIntegral && type2.isIntegral) type1.byteSize - type2.byteSize else type1.ordinal - type2.ordinal
        }

        private fun docForConversionFromFloatingToIntegral(fromFloating: PrimitiveType, toIntegral: PrimitiveType): String {
            require(fromFloating.isFloatingPoint)
            require(toIntegral.isIntegral)

            val thisName = fromFloating.capitalized
            val otherName = toIntegral.capitalized

            return if (compareByDomainCapacity(toIntegral, PrimitiveType.INT) < 0) {
                """
             The resulting `$otherName` value is equal to `this.toInt().to$otherName()`.
            """.trimIndent()
            } else {
                """
             The fractional part, if any, is rounded down towards zero.
             Returns zero if this `$thisName` value is `NaN`, [$otherName.MIN_VALUE] if it's less than `$otherName.MIN_VALUE`,
             [$otherName.MAX_VALUE] if it's bigger than `$otherName.MAX_VALUE`.
            """.trimIndent()
            }
        }

        private fun docForConversionFromFloatingToFloating(fromFloating: PrimitiveType, toFloating: PrimitiveType): String {
            require(fromFloating.isFloatingPoint)
            require(toFloating.isFloatingPoint)

            val thisName = fromFloating.capitalized
            val otherName = toFloating.capitalized

            return if (compareByDomainCapacity(toFloating, fromFloating) < 0) {
                """
             The resulting value is the closest `$otherName` to this `$thisName` value.
             In case when this `$thisName` value is exactly between two `$otherName`s,
             the one with zero at least significant bit of mantissa is selected.
            """.trimIndent()
            } else {
                """
             The resulting `$otherName` value represents the same numerical value as this `$thisName`.
            """.trimIndent()
            }
        }

        private fun docForConversionFromIntegralToIntegral(fromIntegral: PrimitiveType, toIntegral: PrimitiveType): String {
            require(fromIntegral.isIntegral)
            require(toIntegral.isIntegral)

            val thisName = fromIntegral.capitalized
            val otherName = toIntegral.capitalized

            return if (toIntegral == PrimitiveType.CHAR) {
                when (fromIntegral) {
                    PrimitiveType.SHORT -> """
                        The resulting `Char` code is equal to this value reinterpreted as an unsigned number,
                        i.e. it has the same binary representation as this `Short`.
                        """.trimIndent()
                    PrimitiveType.BYTE -> """
                        If this value is non-negative, the resulting `Char` code is equal to this value.
                        
                        The least significant 8 bits of the resulting `Char` code are the same as the bits of this `Byte` value,
                        whereas the most significant 8 bits are filled with the sign bit of this value.
                        """.trimIndent()
                    else -> """
                        If this value is in the range of `Char` codes `Char.MIN_VALUE..Char.MAX_VALUE`,
                        the resulting `Char` code is equal to this value.
                        
                        The resulting `Char` code is represented by the least significant 16 bits of this `$thisName` value.
                        """.trimIndent()
                }
            } else if (compareByDomainCapacity(toIntegral, fromIntegral) < 0) {
                """
             If this value is in [$otherName.MIN_VALUE]..[$otherName.MAX_VALUE], the resulting `$otherName` value represents
             the same numerical value as this `$thisName`.
             
             The resulting `$otherName` value is represented by the least significant ${toIntegral.bitSize} bits of this `$thisName` value.
            """.trimIndent()
            } else {
                """
             The resulting `$otherName` value represents the same numerical value as this `$thisName`.
             
             The least significant ${fromIntegral.bitSize} bits of the resulting `$otherName` value are the same as the bits of this `$thisName` value,
             whereas the most significant ${toIntegral.bitSize - fromIntegral.bitSize} bits are filled with the sign bit of this value.
            """.trimIndent()
            }
        }

        private fun docForConversionFromIntegralToFloating(fromIntegral: PrimitiveType, toFloating: PrimitiveType): String {
            require(fromIntegral.isIntegral)
            require(toFloating.isFloatingPoint)

            val thisName = fromIntegral.capitalized
            val otherName = toFloating.capitalized

            return if (fromIntegral == PrimitiveType.LONG || fromIntegral == PrimitiveType.INT && toFloating == PrimitiveType.FLOAT) {
                """
             The resulting value is the closest `$otherName` to this `$thisName` value.
             In case when this `$thisName` value is exactly between two `$otherName`s,
             the one with zero at least significant bit of mantissa is selected.
            """.trimIndent()
            } else {
                """
             The resulting `$otherName` value represents the same numerical value as this `$thisName`.
            """.trimIndent()
            }
        }

        private fun maxByDomainCapacity(type1: PrimitiveType, type2: PrimitiveType): PrimitiveType {
            return if (type1.ordinal > type2.ordinal) type1 else type2
        }

        fun getOperatorReturnType(kind1: PrimitiveType, kind2: PrimitiveType): PrimitiveType {
            require(kind1 != PrimitiveType.BOOLEAN) { "kind1 must not be BOOLEAN" }
            require(kind2 != PrimitiveType.BOOLEAN) { "kind2 must not be BOOLEAN" }
            return maxByDomainCapacity(maxByDomainCapacity(kind1, kind2), PrimitiveType.INT)
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

    open fun primitiveConstants(type: PrimitiveType): List<Any> = when (type) {
        PrimitiveType.INT -> listOf(java.lang.Integer.MIN_VALUE, java.lang.Integer.MAX_VALUE)
        PrimitiveType.BYTE -> listOf(java.lang.Byte.MIN_VALUE, java.lang.Byte.MAX_VALUE)
        PrimitiveType.SHORT -> listOf(java.lang.Short.MIN_VALUE, java.lang.Short.MAX_VALUE)
        PrimitiveType.LONG -> listOf((java.lang.Long.MIN_VALUE + 1).toString() + "L - 1L", java.lang.Long.MAX_VALUE.toString() + "L")
        PrimitiveType.DOUBLE -> listOf(java.lang.Double.MIN_VALUE, java.lang.Double.MAX_VALUE, "1.0/0.0", "-1.0/0.0", "-(0.0/0.0)")
        PrimitiveType.FLOAT -> listOf(java.lang.Float.MIN_VALUE, java.lang.Float.MAX_VALUE, "1.0F/0.0F", "-1.0F/0.0F", "-(0.0F/0.0F)").map { it as? String ?: "${it}F" }
        else -> throw IllegalArgumentException("type: $type")
    }

    open fun PrimitiveType.shouldGenerate(): Boolean = true

    override fun generate() {
        writer.print(generateFile().build())
    }

    private fun generateFile(): FileBuilder {
        return file { generateClasses() }.apply { this.modifyGeneratedFile() }
    }

    private fun FileBuilder.generateClasses() {
        for (thisKind in PrimitiveType.onlyNumeric.filter { it.shouldGenerate() }) {
            val className = thisKind.capitalized

            klass {
                appendDoc("Represents a ${typeDescriptions[thisKind]}.")
                name = className
                generateCompanionObject(thisKind)

                generateCompareTo(thisKind)
                generateBinaryOperators(thisKind)
                generateUnaryOperators(thisKind)
                generateRangeTo(thisKind)
                generateRangeUntil(thisKind)

                if (thisKind == PrimitiveType.INT || thisKind == PrimitiveType.LONG) {
                    generateBitShiftOperators(thisKind)
                    generateBitwiseOperators(thisKind)
                }

                generateConversions(thisKind)
                generateToString(thisKind)
                generateEquals(thisKind)
                generateAdditionalMethods(thisKind)
            }.modifyGeneratedClass(thisKind)
        }
    }

    private fun ClassBuilder.generateCompanionObject(thisKind: PrimitiveType) {
        companionObject {
            val className = thisKind.capitalized
            if (thisKind == PrimitiveType.FLOAT || thisKind == PrimitiveType.DOUBLE) {
                val (minValue, maxValue, posInf, negInf, nan) = primitiveConstants(thisKind)
                property {
                    appendDoc("A constant holding the smallest *positive* nonzero value of $className.")
                    name = "MIN_VALUE"
                    type = className
                    value = minValue.toString()
                }.modifyGeneratedCompanionObjectProperty(thisKind)

                property {
                    appendDoc("A constant holding the largest positive finite value of $className.")
                    name = "MAX_VALUE"
                    type = className
                    value = maxValue.toString()
                }.modifyGeneratedCompanionObjectProperty(thisKind)

                property {
                    appendDoc("A constant holding the positive infinity value of $className.")
                    name = "POSITIVE_INFINITY"
                    type = className
                    value = posInf.toString()
                }.modifyGeneratedCompanionObjectProperty(thisKind)

                property {
                    appendDoc("A constant holding the negative infinity value of $className.")
                    name = "NEGATIVE_INFINITY"
                    type = className
                    value = negInf.toString()
                }.modifyGeneratedCompanionObjectProperty(thisKind)

                property {
                    appendDoc("A constant holding the \"not a number\" value of $className.")
                    name = "NaN"
                    type = className
                    value = nan.toString()
                }.modifyGeneratedCompanionObjectProperty(thisKind)
            }

            if (thisKind == PrimitiveType.INT || thisKind == PrimitiveType.LONG || thisKind == PrimitiveType.SHORT || thisKind == PrimitiveType.BYTE) {
                val (minValue, maxValue) = primitiveConstants(thisKind)
                property {
                    appendDoc("A constant holding the minimum value an instance of $className can have.")
                    name = "MIN_VALUE"
                    type = className
                    value = minValue.toString()
                }.modifyGeneratedCompanionObjectProperty(thisKind)

                property {
                    appendDoc("A constant holding the maximum value an instance of $className can have.")
                    name = "MAX_VALUE"
                    type = className
                    value = maxValue.toString()
                }.modifyGeneratedCompanionObjectProperty(thisKind)
            }

            val sizeSince = if (thisKind.isFloatingPoint) "1.4" else "1.3"
            property {
                appendDoc("The number of bytes used to represent an instance of $className in a binary form.")
                annotations += mutableListOf("SinceKotlin(\"$sizeSince\")")
                name = "SIZE_BYTES"
                type = "Int"
                value = thisKind.byteSize.toString()
            }.modifyGeneratedCompanionObjectProperty(thisKind)

            property {
                appendDoc("The number of bits used to represent an instance of $className in a binary form.")
                annotations += mutableListOf("SinceKotlin(\"$sizeSince\")")
                name = "SIZE_BITS"
                type = "Int"
                value = thisKind.bitSize.toString()
            }.modifyGeneratedCompanionObjectProperty(thisKind)
        }.modifyGeneratedCompanionObject(thisKind)
    }

    private fun ClassBuilder.generateCompareTo(thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            val doc = """
                    Compares this value with the specified value for order.
                    Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
                    or a positive number if it's greater than other.
                """.trimIndent()

            method {
                appendDoc(doc)
                annotations += "kotlin.internal.IntrinsicConstEvaluation"
                signature {
                    isOverride = otherKind == thisKind
                    isOperator = true
                    methodName = "compareTo"
                    parameter {
                        name = "other"
                        type = otherKind.capitalized
                    }
                    returnType = PrimitiveType.INT.capitalized
                }
            }.modifyGeneratedCompareTo(thisKind, otherKind)
        }
    }

    private fun ClassBuilder.generateBinaryOperators(thisKind: PrimitiveType) {
        for (name in binaryOperators) {
            generateOperator(name, thisKind)
        }
    }

    private fun ClassBuilder.generateOperator(operatorName: String, thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            val opReturnType = getOperatorReturnType(thisKind, otherKind)

            val annotationsToAdd = buildList {
                if (operatorName == "rem") add("SinceKotlin(\"1.1\")")
                add("kotlin.internal.IntrinsicConstEvaluation")
            }

            method {
                appendDoc(binaryOperatorDoc(operatorName, thisKind, otherKind))
                annotations += annotationsToAdd
                signature {
                    isOperator = true
                    methodName = operatorName
                    parameter {
                        name = "other"
                        type = otherKind.capitalized
                    }
                    returnType = opReturnType.capitalized
                }
            }.modifyGeneratedBinaryOperation(thisKind, otherKind)
        }
    }

    private fun ClassBuilder.generateUnaryOperators(thisKind: PrimitiveType) {
        for (operatorName in listOf("inc", "dec")) {
            method {
                appendDoc(incDecOperatorsDoc(operatorName))
                signature {
                    isOperator = true
                    methodName = operatorName
                    returnType = thisKind.capitalized
                }
            }.modifyGeneratedUnaryOperation(thisKind)
        }

        for ((operatorName, doc) in unaryPlusMinusOperators) {
            val opReturnType = when (thisKind) {
                in listOf(PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR) -> PrimitiveType.INT.capitalized
                else -> thisKind.capitalized
            }

            method {
                appendDoc(doc)
                annotations += "kotlin.internal.IntrinsicConstEvaluation"
                signature {
                    isOperator = true
                    methodName = operatorName
                    returnType = opReturnType
                }
            }.modifyGeneratedUnaryOperation(thisKind)
        }
    }

    private fun ClassBuilder.generateRangeTo(thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            val opReturnType = maxByDomainCapacity(maxByDomainCapacity(thisKind, otherKind), PrimitiveType.INT)

            if (opReturnType == PrimitiveType.DOUBLE || opReturnType == PrimitiveType.FLOAT) {
                continue
            }

            method {
                appendDoc("Creates a range from this value to the specified [other] value.")
                signature {
                    isOperator = true
                    methodName = "rangeTo"
                    parameter {
                        name = "other"
                        type = otherKind.capitalized
                    }
                    returnType = "${opReturnType.capitalized}Range"
                }
            }.modifyGeneratedRangeTo(thisKind)
        }
    }

    private fun ClassBuilder.generateRangeUntil(thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            val opReturnType = maxByDomainCapacity(maxByDomainCapacity(thisKind, otherKind), PrimitiveType.INT)

            if (opReturnType == PrimitiveType.DOUBLE || opReturnType == PrimitiveType.FLOAT) {
                continue
            }

            method {
                appendDoc(
                    """
                        Creates a range from this value up to but excluding the specified [other] value.
                        
                        If the [other] value is less than or equal to `this` value, then the returned range is empty.
                    """.trimIndent()
                )
                annotations += mutableListOf("SinceKotlin(\"1.9\")", "WasExperimental(ExperimentalStdlibApi::class)")
                signature {
                    isOperator = true
                    methodName = "rangeUntil"
                    parameter {
                        name = "other"
                        type = otherKind.capitalized
                    }
                    returnType = "${opReturnType.capitalized}Range"
                }
            }.modifyGeneratedRangeUntil(thisKind)
        }
    }

    private fun ClassBuilder.generateBitShiftOperators(thisKind: PrimitiveType) {
        val className = thisKind.capitalized
        val detail = shiftOperatorsDocDetail(thisKind)
        for ((operatorName, doc) in shiftOperators) {
            method {
                appendDoc(doc + END_LINE + END_LINE + detail)
                annotations += "kotlin.internal.IntrinsicConstEvaluation"
                signature {
                    isInfix = true
                    methodName = operatorName
                    parameter {
                        name = "bitCount"
                        type = PrimitiveType.INT.capitalized
                    }
                    returnType = className
                }
            }.modifyGeneratedBitShiftOperators(thisKind)
        }
    }

    private fun ClassBuilder.generateBitwiseOperators(thisKind: PrimitiveType) {
        for ((operatorName, doc) in bitwiseOperators) {
            method {
                appendDoc(doc)
                annotations += "kotlin.internal.IntrinsicConstEvaluation"
                signature {
                    isInfix = true
                    methodName = operatorName
                    parameter {
                        name = "other"
                        type = thisKind.capitalized
                    }
                    returnType = thisKind.capitalized
                }

            }.modifyGeneratedBitwiseOperators(thisKind)
        }

        method {
            appendDoc("Inverts the bits in this value.")
            annotations += "kotlin.internal.IntrinsicConstEvaluation"
            signature {
                methodName = "inv"
                returnType = thisKind.capitalized
            }
        }.modifyGeneratedBitwiseOperators(thisKind)
    }

    private fun ClassBuilder.generateConversions(thisKind: PrimitiveType) {
        fun isFpToIntConversionDeprecated(otherKind: PrimitiveType): Boolean {
            return thisKind in PrimitiveType.floatingPoint && otherKind in listOf(PrimitiveType.BYTE, PrimitiveType.SHORT)
        }

        fun isCharConversionDeprecated(otherKind: PrimitiveType): Boolean {
            return thisKind != PrimitiveType.INT && otherKind == PrimitiveType.CHAR
        }

        val thisName = thisKind.capitalized
        for (otherKind in PrimitiveType.exceptBoolean) {
            val otherName = otherKind.capitalized
            val doc = if (thisKind == otherKind) {
                "Returns this value."
            } else {
                val detail = if (thisKind in PrimitiveType.integral) {
                    if (otherKind.isIntegral) {
                        docForConversionFromIntegralToIntegral(thisKind, otherKind)
                    } else {
                        docForConversionFromIntegralToFloating(thisKind, otherKind)
                    }
                } else {
                    if (otherKind.isIntegral) {
                        docForConversionFromFloatingToIntegral(thisKind, otherKind)
                    } else {
                        docForConversionFromFloatingToFloating(thisKind, otherKind)
                    }
                }

                "Converts this [$thisName] value to [$otherName].$END_LINE$END_LINE$detail"
            }

            val annotationsToAdd = mutableListOf<String>()
            if (isFpToIntConversionDeprecated(otherKind)) {
                annotationsToAdd += "Deprecated(\"Unclear conversion. To achieve the same result convert to Int explicitly and then to $otherName.\", ReplaceWith(\"toInt().to$otherName()\"))"
                annotationsToAdd += "DeprecatedSinceKotlin(warningSince = \"1.3\", errorSince = \"1.5\")"
            }
            if (isCharConversionDeprecated(otherKind)) {
                annotationsToAdd += "Deprecated(\"Direct conversion to Char is deprecated. Use toInt().toChar() or Char constructor instead.\", ReplaceWith(\"this.toInt().toChar()\"))"
                annotationsToAdd += "DeprecatedSinceKotlin(warningSince = \"1.5\", errorSince = \"2.3\")"
            }
            if (thisKind == PrimitiveType.INT && otherKind == PrimitiveType.CHAR) {
                annotationsToAdd += "Suppress(\"OVERRIDE_DEPRECATION\")"
            }

            annotationsToAdd += "kotlin.internal.IntrinsicConstEvaluation"
            method {
                appendDoc(doc)
                annotations += annotationsToAdd
                signature {
                    isOverride = true
                    methodName = "to$otherName"
                    returnType = otherName
                }
            }.modifyGeneratedConversions(thisKind)
        }
    }

    private fun ClassBuilder.generateEquals(thisKind: PrimitiveType) {
        method {
            annotations += "kotlin.internal.IntrinsicConstEvaluation"
            signature {
                isOverride = true
                methodName = "equals"
                parameter {
                    name = "other"
                    type = "Any?"
                }
                returnType = "Boolean"
            }
        }.modifyGeneratedEquals(thisKind)
    }

    private fun ClassBuilder.generateToString(thisKind: PrimitiveType) {
        method {
            annotations += "kotlin.internal.IntrinsicConstEvaluation"
            signature {
                isOverride = true
                methodName = "toString"
                returnType = "String"
            }
        }.modifyGeneratedToString(thisKind)
    }

    internal open fun FileBuilder.modifyGeneratedFile() {}
    internal open fun ClassBuilder.modifyGeneratedClass(thisKind: PrimitiveType) {}
    internal open fun CompanionObjectBuilder.modifyGeneratedCompanionObject(thisKind: PrimitiveType) {}
    internal open fun PropertyBuilder.modifyGeneratedCompanionObjectProperty(thisKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedCompareTo(thisKind: PrimitiveType, otherKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedBinaryOperation(thisKind: PrimitiveType, otherKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedUnaryOperation(thisKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedRangeTo(thisKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedRangeUntil(thisKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedBitShiftOperators(thisKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedBitwiseOperators(thisKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedConversions(thisKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedEquals(thisKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedToString(thisKind: PrimitiveType) {}
    internal open fun ClassBuilder.generateAdditionalMethods(thisKind: PrimitiveType) {}
}

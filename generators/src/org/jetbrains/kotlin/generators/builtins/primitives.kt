/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.generators.builtins.ranges

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import java.io.PrintWriter

class GeneratePrimitives(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    private val binaryOperators: Map<String, String> = mapOf(
            "plus" to "Adds the other value to this value.",
            "minus" to "Subtracts the other value from this value.",
            "times" to "Multiplies this value by the other value.",
            "div" to "Divides this value by the other value.",
            "mod" to "Calculates the remainder of dividing this value by the other value."
    )
    private val unaryOperators: Map<String, String> = mapOf(
            "inc" to "Increments this value.",
            "dec" to "Decrements this value.",
            "unaryPlus" to "Returns this value.",
            "unaryMinus" to "Returns the negative of this value.")
    private val shiftOperators: Map<String, String> = mapOf(
            "shl" to "Shifts this value left by [bits].",
            "shr" to "Shifts this value right by [bits], filling the leftmost bits with copies of the sign bit.",
            "ushr" to "Shifts this value right by [bits], filling the leftmost bits with zeros.")
    private val bitwiseOperators: Map<String, String> = mapOf(
            "and" to "Performs a bitwise AND operation between the two values.",
            "or" to "Performs a bitwise OR operation between the two values.",
            "xor" to "Performs a bitwise XOR operation between the two values.")
    private val typeDescriptions: Map<PrimitiveType, String> = hashMapOf(
            PrimitiveType.DOUBLE to "double-precision 64-bit IEEE 754 floating point number",
            PrimitiveType.FLOAT to "single-precision 32-bit IEEE 754 floating point number",
            PrimitiveType.LONG to "64-bit signed integer",
            PrimitiveType.INT to "32-bit signed integer",
            PrimitiveType.SHORT to "16-bit signed integer",
            PrimitiveType.BYTE to "8-bit signed integer",
            PrimitiveType.CHAR to "16-bit Unicode character"
    )

    override fun generateBody() {
        for (kind in PrimitiveType.onlyNumeric) {
            val className = kind.capitalized
            generateDoc(kind)
            out.println("public class $className private () : Number, Comparable<$className> {")

            out.print("    companion object")
            if (kind == PrimitiveType.FLOAT || kind == PrimitiveType.DOUBLE) {
                out.print(" : FloatingPointConstants<$className>")
            }
            if (kind == PrimitiveType.INT || kind == PrimitiveType.LONG || kind == PrimitiveType.SHORT || kind == PrimitiveType.BYTE) {
                out.print(" : IntegerConstants<$className>")
            }
            out.println(" {}\n")

            generateCompareTo(kind)

            generateBinaryOperators(kind)
            generateUnaryOperators(kind)
            generateRangeTo(kind)

            if (kind == PrimitiveType.INT || kind == PrimitiveType.LONG) {
                generateBitwiseOperators(className)
            }

            generateConversions(kind)

            out.println("}\n")
        }
    }

    private fun generateDoc(kind: PrimitiveType) {
        out.println("/**")
        out.println(" * Represents a ${typeDescriptions[kind]}.")
        out.println(" * On the JVM, non-nullable values of this type are represented as values of the primitive type `${kind.name().toLowerCase()}`.")
        out.println(" */")
    }

    private fun generateCompareTo(thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            out.println("/**")
            out.println(" * Compares this value with the specified value for order.")
            out.println(" * Returns zero if this value is equal to the specified other value, a negative number if its less than other, ")
            out.println(" * or a positive number if its greater than other.")
            out.println(" */")
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
            out.println("    public operator fun $name(other: ${otherKind.capitalized}): ${returnType.capitalized}")
        }
        out.println()
    }

    private fun generateRangeTo(thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.onlyNumeric) {
            val returnType =
                maxByDomainCapacity(thisKind, otherKind)
                .let { if (it == PrimitiveType.CHAR) it else maxByDomainCapacity(it, PrimitiveType.INT) }
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

    private fun generateBitwiseOperators(className: String) {
        for ((name, doc) in shiftOperators) {
            out.println("    /** $doc */")
            out.println("    public infix fun $name(bitCount: Int): $className")
        }
        for ((name, doc) in bitwiseOperators) {
            out.println("    /** $doc */")
            out.println("    public infix fun $name(other: $className): $className")
        }
        out.println("    /** Inverts the bits in this value/ */")
        out.println("    public fun inv(): $className")
        out.println()
    }

    private fun generateConversions(kind: PrimitiveType) {
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

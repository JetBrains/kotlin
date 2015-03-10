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

class GenerateNumbers(out: PrintWriter) : BuiltInsSourceGenerator(out) {
    private val binaryOperators: List<String> = listOf("plus", "minus", "times", "div", "mod")
    private val unaryOperators: List<String> = listOf("inc", "dec", "plus", "minus")
    private val shiftOperators: List<String> = listOf("shl", "shr", "ushr")
    private val bitwiseOperators: List<String> = listOf("and", "or", "xor")
    private val typeDescriptions: Map<PrimitiveType, String> = hashMapOf(
            PrimitiveType.DOUBLE to "double-precision 64-bit IEEE 754 floating point number",
            PrimitiveType.FLOAT to "single-precision 32-bit IEEE 754 floating point number",
            PrimitiveType.LONG to "64-bit signed integer",
            PrimitiveType.INT to "32-bit signed integer",
            PrimitiveType.SHORT to "16-bit signed integer",
            PrimitiveType.BYTE to "8-bit signed integer"
    )


    override fun generateBody() {
        for (kind in PrimitiveType.values()) {
            if (kind == PrimitiveType.BOOLEAN || kind == PrimitiveType.CHAR) continue
            val className = kind.capitalized
            generateDoc(kind)
            out.println("public class $className private () : Number, Comparable<$className> {")

            out.print("    default object")
            if (kind == PrimitiveType.FLOAT || kind == PrimitiveType.DOUBLE) {
                out.print(" : FloatingPointConstants<$className>")
            }
            out.println(" {}\n")

            generateCompareTo(kind)

            generateBinaryOperators(kind)
            generateUnaryOperators(kind)
            generateRangeTo(kind)

            if (kind == PrimitiveType.INT || kind == PrimitiveType.LONG) {
                generateBitwiseOperators(className)
            }

            generateConversions()

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
        for (otherKind in PrimitiveType.exceptBoolean) {
            out.print("    public ")
            if (otherKind == thisKind) out.print("override ")
            out.println("fun compareTo(other: ${otherKind.capitalized}): Int")
        }
        out.println()
    }

    private fun generateBinaryOperators(thisKind: PrimitiveType) {
        for (name in binaryOperators) {
            generateOperator(name, thisKind)
        }
    }

    private fun generateOperator(name: String, thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.exceptBoolean) {
            val returnType = getOperatorReturnType(thisKind, otherKind)
            out.println("    public fun $name(other: ${otherKind.capitalized}): $returnType")
        }
        out.println()
    }

    private fun generateRangeTo(thisKind: PrimitiveType) {
        for (otherKind in PrimitiveType.exceptBoolean) {
            val returnType = if (otherKind.ordinal() > thisKind.ordinal()) otherKind else thisKind
            out.println("    public fun rangeTo(other: ${otherKind.capitalized}): ${returnType.capitalized}Range")
        }
        out.println()

    }

    private fun generateUnaryOperators(kind: PrimitiveType) {
        for (name in unaryOperators) {
            val returnType = if (kind in listOf(PrimitiveType.SHORT, PrimitiveType.BYTE) &&
                                 name in listOf("plus", "minus")) "Int" else kind.capitalized
            out.println("    public fun $name(): $returnType")
        }
        out.println()
    }

    private fun generateBitwiseOperators(className: String) {
        for (name in shiftOperators) {
            out.println("    public fun $name(bits: Int): $className")
        }
        for (name in bitwiseOperators) {
            out.println("    public fun $name(other: $className): $className")
        }
        out.println("    public fun inv(): $className")
        out.println()
    }

    private fun generateConversions() {
        for (otherKind in PrimitiveType.exceptBoolean) {
            val name = otherKind.capitalized
            out.println("    public override fun to$name(): $name")
        }
    }

    private fun getOperatorReturnType(kind1: PrimitiveType, kind2: PrimitiveType): String {
        if (kind1 == PrimitiveType.DOUBLE || kind2 == PrimitiveType.DOUBLE) return "Double"
        if (kind1 == PrimitiveType.FLOAT || kind2 == PrimitiveType.FLOAT) return "Float"
        if (kind1 == PrimitiveType.LONG || kind2 == PrimitiveType.LONG) return "Long"
        return "Int"
    }
}

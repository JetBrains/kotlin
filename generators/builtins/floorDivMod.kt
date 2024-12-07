/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.convert
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsSourceGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.BasePrimitivesGenerator
import org.jetbrains.kotlin.generators.builtins.printDoc
import java.io.PrintWriter

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
        out.printDoc(BasePrimitivesGenerator.binaryOperatorDoc("floorDiv", thisKind, otherKind), "")
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
        out.printDoc(BasePrimitivesGenerator.binaryOperatorDoc("mod", thisKind, otherKind),"")
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
        out.printDoc(BasePrimitivesGenerator.binaryOperatorDoc("mod", thisKind, otherKind), "")
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


/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.unsigned


import org.jetbrains.kotlin.generators.builtins.UnsignedType
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
    }


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
            out.println("operator fun compareTo(other: ${otherType.capitalized}): Int = TODO()")
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
        // TODO("not implemented")
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
            out.println("    public fun to$signed(): $signed = TODO()")
        }
        out.println()

        for (otherType in UnsignedType.values()) {
            val name = otherType.capitalized
            out.println("    public fun to$name(): $name = TODO()")
        }
        out.println()
    }

    private fun generateExtensionConversions() {
        for (otherType in UnsignedType.values()) {
            val otherSigned = otherType.asSigned.capitalized
            val thisSigned = type.asSigned.capitalized
            out.println("public fun $otherSigned.to$className(): $className = $className(this.to$thisSigned())")
        }
    }


    private fun maxByDomainCapacity(type1: UnsignedType, type2: UnsignedType): UnsignedType =
        if (type1.ordinal > type2.ordinal) type1 else type2


    private fun getOperatorReturnType(type1: UnsignedType, type2: UnsignedType): UnsignedType {
        return maxByDomainCapacity(maxByDomainCapacity(type1, type2), UnsignedType.UINT)
    }

}

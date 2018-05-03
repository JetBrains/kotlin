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
        generate(File(targetDir, "kotlin/${type.capitalized}Array.kt")) { UnsignedArrayGenerator(type, it) }
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

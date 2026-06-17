/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.unsigned

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.convert
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.BasePrimitivesGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ClassBuilder
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ClassModifier
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.END_LINE
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.FileBuilder
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.MethodVisibility
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.file
import java.io.File
import java.io.PrintWriter

private const val INLINE_ONLY = "kotlin.internal.InlineOnly"
private const val PUBLISHED_API = "PublishedApi"
private const val INTRINSIC_CONST_EVALUATION = "kotlin.internal.IntrinsicConstEvaluation"

enum class Target {
    COMMON,
    JVM,
    JS,
    NATIVE,
    WASM
}

fun generateUnsignedTypes(
    targetDir: File,
    target: Target,
    generate: (File, (PrintWriter) -> BuiltInsGenerator) -> Unit
) {
    if (target != Target.COMMON) return
    val prefix = if (targetDir.path.contains("kotlin/")) "" else "kotlin/"

    for (type in UnsignedType.entries) {
        generate(File(targetDir, "$prefix${type.capitalized}.kt")) { UnsignedTypeGenerator(type, target, it) }
        generate(File(targetDir, "$prefix${type.capitalized}Array.kt")) { UnsignedArrayGenerator(type, target, it) }
    }

    for (type in listOf(UnsignedType.UINT, UnsignedType.ULONG)) {
        generate(File(targetDir, "$prefix${type.capitalized}Range.kt")) { UnsignedRangeGenerator(type, target, it) }
    }
}

class UnsignedTypeGenerator(val type: UnsignedType, val target: Target, private val out: PrintWriter) : BuiltInsGenerator {
    private val className = type.capitalized
    private val storageType = type.asSigned.capitalized

    internal fun binaryOperatorDoc(operator: String, operand1: UnsignedType, operand2: UnsignedType): String = when (operator) {
        "floorDiv" ->
            """
            Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.

            For unsigned types, the results of flooring division and truncating division are the same.
            @sample samples.misc.Builtins.floorDivUnsigned
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
                @sample samples.misc.Builtins.modUnsigned
                """.trimIndent()
        }
        else -> BasePrimitivesGenerator.binaryOperatorDoc(operator, operand1.asSigned, operand2.asSigned)
    }

    override fun generate() {
        out.print(buildFile().build())
    }

    private fun buildFile(): FileBuilder = file(this::class) {
        import("kotlin.experimental.*")
        import("kotlin.jvm.*")

        klass {
            annotations += "SinceKotlin(\"1.5\")"
            annotations += "JvmInline"
            name = className
            modifier(ClassModifier.VALUE)
            superType("Comparable<$className>")
            primaryConstructor {
                visibility = MethodVisibility.INTERNAL
                annotations += PUBLISHED_API
                annotations += INTRINSIC_CONST_EVALUATION
                propertyParameter {
                    annotations += PUBLISHED_API
                    visibility = MethodVisibility.INTERNAL
                    name = "data"
                    type = storageType
                }
            }

            generateCompanionObject()
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
        }

        generateExtensionConversions()
    }

    private fun ClassBuilder.generateCompanionObject() {
        val sizeBytes = type.byteSize
        val sizeBits = type.byteSize * 8
        companionObject {
            constProperty {
                appendDoc("A constant holding the minimum value an instance of $className can have.")
                name = "MIN_VALUE"
                type = className
                value = "$className(0)"
            }
            constProperty {
                appendDoc("A constant holding the maximum value an instance of $className can have.")
                name = "MAX_VALUE"
                type = className
                value = "$className(-1)"
            }
            constProperty {
                appendDoc("The number of bytes used to represent an instance of $className in a binary form.")
                name = "SIZE_BYTES"
                type = "Int"
                value = "$sizeBytes"
            }
            constProperty {
                appendDoc("The number of bits used to represent an instance of $className in a binary form.")
                name = "SIZE_BITS"
                type = "Int"
                value = "$sizeBits"
            }
        }
    }

    private fun ClassBuilder.generateCompareTo() {
        for (otherType in UnsignedType.entries) {
            method {
                appendDoc(
                    """
                    Compares this value with the specified value for order.
                    Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
                    or a positive number if it's greater than other.
                    """.trimIndent()
                )
                annotations += INLINE_ONLY
                annotations += INTRINSIC_CONST_EVALUATION
                if (otherType == type) annotations += "Suppress(\"OVERRIDE_BY_INLINE\")"
                signature {
                    isInline = true
                    isOperator = true
                    isOverride = otherType == type
                    methodName = "compareTo"
                    parameter { name = "other"; type = otherType.capitalized }
                    returnType = "Int"
                }
                val body = if (otherType == type && maxByDomainCapacity(type, UnsignedType.UINT) == type) {
                    "${className.lowercase()}Compare(this.data, other.data)"
                } else {
                    if (maxOf(type, otherType) < UnsignedType.UINT) {
                        "this.toInt().compareTo(other.toInt())"
                    } else {
                        val ctype = maxByDomainCapacity(type, otherType)
                        "${convert("this", type, ctype)}.compareTo(${convert("other", otherType, ctype)})"
                    }
                }
                body.setAsExpressionBody()
            }
        }
    }

    private fun ClassBuilder.generateBinaryOperators() {
        for (name in BasePrimitivesGenerator.binaryOperators) {
            generateOperator(name)
        }
        generateFloorDivMod("floorDiv")
        generateFloorDivMod("mod")
    }

    private fun ClassBuilder.generateOperator(name: String) {
        for (otherType in UnsignedType.entries) {
            val opReturnType = getOperatorReturnType(type, otherType)
            method {
                appendDoc(binaryOperatorDoc(name, type, otherType))
                annotations += INLINE_ONLY
                annotations += INTRINSIC_CONST_EVALUATION
                signature {
                    isInline = true
                    isOperator = true
                    methodName = name
                    parameter { this.name = "other"; type = otherType.capitalized }
                    returnType = opReturnType.capitalized
                }
                val body = if (type == otherType && type == opReturnType) {
                    when (name) {
                        "plus", "minus", "times" -> "$className(this.data.$name(other.data))"
                        "div" -> "${type.capitalized.lowercase()}Divide(this, other)"
                        "rem" -> "${type.capitalized.lowercase()}Remainder(this, other)"
                        else -> error(name)
                    }
                } else {
                    "${convert("this", type, opReturnType)}.$name(${convert("other", otherType, opReturnType)})"
                }
                body.setAsExpressionBody()
            }
        }
    }

    private fun ClassBuilder.generateFloorDivMod(name: String) {
        for (otherType in UnsignedType.entries) {
            val operationType = getOperatorReturnType(type, otherType)
            val opReturnType = if (name == "mod") otherType else operationType
            method {
                appendDoc(binaryOperatorDoc(name, type, otherType))
                annotations += INLINE_ONLY
                annotations += INTRINSIC_CONST_EVALUATION
                signature {
                    isInline = true
                    methodName = name
                    parameter { this.name = "other"; type = otherType.capitalized }
                    returnType = opReturnType.capitalized
                }
                val body = if (type == otherType && type == operationType) {
                    when (name) {
                        "floorDiv" -> "div(other)"
                        "mod" -> "rem(other)"
                        else -> error(name)
                    }
                } else {
                    convert(
                        "${convert("this", type, operationType)}.$name(${convert("other", otherType, operationType)})",
                        operationType, opReturnType
                    )
                }
                body.setAsExpressionBody()
            }
        }
    }

    private fun ClassBuilder.generateUnaryOperators() {
        for (name in listOf("inc", "dec")) {
            method {
                appendDoc(BasePrimitivesGenerator.incDecOperatorsDoc(name))
                annotations += INLINE_ONLY
                signature {
                    isInline = true
                    isOperator = true
                    methodName = name
                    returnType = className
                }
                "$className(data.$name())".setAsExpressionBody()
            }
        }
    }

    private fun ClassBuilder.generateRangeTo() {
        val rangeElementType = maxByDomainCapacity(type, UnsignedType.UINT)
        val rangeType = rangeElementType.capitalized + "Range"
        fun convertToRangeElement(name: String) = if (rangeElementType == type) name else "$name.to${rangeElementType.capitalized}()"
        method {
            appendDoc("Creates a range from this value to the specified [other] value.")
            annotations += INLINE_ONLY
            signature {
                isInline = true
                isOperator = true
                methodName = "rangeTo"
                parameter { name = "other"; type = className }
                returnType = rangeType
            }
            "$rangeType(${convertToRangeElement("this")}, ${convertToRangeElement("other")})".setAsExpressionBody()
        }
    }

    private fun ClassBuilder.generateRangeUntil() {
        val rangeElementType = maxByDomainCapacity(type, UnsignedType.UINT)
        val rangeType = rangeElementType.capitalized + "Range"
        fun convertToRangeElement(name: String) = if (rangeElementType == type) name else "$name.to${rangeElementType.capitalized}()"
        method {
            appendDoc(
                """
                Creates a range from this value up to but excluding the specified [other] value.

                If the [other] value is less than or equal to `this` value, then the returned range is empty.
                """.trimIndent()
            )
            annotations += "SinceKotlin(\"1.9\")"
            annotations += "WasExperimental(ExperimentalStdlibApi::class)"
            annotations += INLINE_ONLY
            signature {
                isInline = true
                isOperator = true
                methodName = "rangeUntil"
                parameter { name = "other"; type = className }
                returnType = rangeType
            }
            "${convertToRangeElement("this")} until ${convertToRangeElement("other")}".setAsExpressionBody()
        }
    }

    private fun ClassBuilder.generateBitShiftOperators() {
        fun generateShiftOperator(name: String, implementation: String = name) {
            val doc = BasePrimitivesGenerator.shiftOperators[implementation]!!
            val detail = BasePrimitivesGenerator.shiftOperatorsDocDetail(type.asSigned)
            method {
                appendDoc(doc + END_LINE + END_LINE + detail)
                annotations += INLINE_ONLY
                annotations += INTRINSIC_CONST_EVALUATION
                signature {
                    isInline = true
                    isInfix = true
                    methodName = name
                    parameter { this.name = "bitCount"; type = "Int" }
                    returnType = className
                }
                "$className(data $implementation bitCount)".setAsExpressionBody()
            }
        }

        generateShiftOperator("shl")
        generateShiftOperator("shr", "ushr")
    }

    private fun ClassBuilder.generateBitwiseOperators() {
        for (entry in BasePrimitivesGenerator.bitwiseOperators) {
            val name = entry.key
            val doc = entry.value
            method {
                appendDoc(doc)
                annotations += INLINE_ONLY
                annotations += INTRINSIC_CONST_EVALUATION
                signature {
                    isInline = true
                    isInfix = true
                    methodName = name
                    parameter { this.name = "other"; type = className }
                    returnType = className
                }
                "$className(this.data $name other.data)".setAsExpressionBody()
            }
        }
        method {
            appendDoc("Inverts the bits in this value.")
            annotations += INLINE_ONLY
            annotations += INTRINSIC_CONST_EVALUATION
            signature {
                isInline = true
                methodName = "inv"
                returnType = className
            }
            "$className(data.inv())".setAsExpressionBody()
        }
    }

    private fun lsb(count: Int) = "least significant $count bits"
    private fun msb(count: Int) = "most significant $count bits"

    private fun ClassBuilder.generateMemberConversions() {
        for (otherType in UnsignedType.entries) {
            val signed = otherType.asSigned.capitalized
            val doc = buildList {
                add("Converts this [$className] value to [$signed].")
                add("")
                when {
                    otherType < type -> {
                        add("If this value is less than or equals to [$signed.MAX_VALUE], the resulting `$signed` value represents")
                        add("the same numerical value as this `$className`.")
                        add("")
                        add("The resulting `$signed` value is represented by the ${lsb(otherType.bitSize)} of this `$className` value.")
                        add("Note that the resulting `$signed` value may be negative.")
                    }
                    otherType == type -> {
                        add("If this value is less than or equals to [$signed.MAX_VALUE], the resulting `$signed` value represents")
                        add("the same numerical value as this `$className`. Otherwise the result is negative.")
                        add("")
                        add("The resulting `$signed` value has the same binary representation as this `$className` value.")
                    }
                    else -> {
                        add("The resulting `$signed` value represents the same numerical value as this `$className`.")
                        add("")
                        add("The ${lsb(type.bitSize)} of the resulting `$signed` value are the same as the bits of this `$className` value,")
                        add("whereas the ${msb(otherType.bitSize - type.bitSize)} are filled with zeros.")
                    }
                }
            }.joinToString(END_LINE)

            method {
                appendDoc(doc)
                annotations += INLINE_ONLY
                annotations += INTRINSIC_CONST_EVALUATION
                signature {
                    isInline = true
                    methodName = "to$signed"
                    returnType = signed
                }
                val body = when {
                    type == UnsignedType.UINT && otherType == UnsignedType.ULONG -> "uintToLong(data)"
                    otherType < type -> "data.to$signed()"
                    otherType == type -> "data"
                    else -> "data.to$signed() and ${type.mask}"
                }
                body.setAsExpressionBody()
            }
        }

        for (otherType in UnsignedType.entries) {
            val name = otherType.capitalized
            val doc = if (type == otherType) {
                "Returns this value."
            } else {
                buildList {
                    add("Converts this [$className] value to [$name].")
                    add("")
                    when {
                        otherType < type -> {
                            add("If this value is less than or equals to [$name.MAX_VALUE], the resulting `$name` value represents")
                            add("the same numerical value as this `$className`.")
                            add("")
                            add("The resulting `$name` value is represented by the ${lsb(otherType.bitSize)} of this `$className` value.")
                        }
                        else -> {
                            add("The resulting `$name` value represents the same numerical value as this `$className`.")
                            add("")
                            add("The ${lsb(type.bitSize)} of the resulting `$name` value are the same as the bits of this `$className` value,")
                            add("whereas the ${msb(otherType.bitSize - type.bitSize)} are filled with zeros.")
                        }
                    }
                }.joinToString(END_LINE)
            }

            method {
                appendDoc(doc)
                annotations += INLINE_ONLY
                annotations += INTRINSIC_CONST_EVALUATION
                signature {
                    isInline = true
                    methodName = "to$name"
                    returnType = name
                }
                val body = when {
                    type == UnsignedType.UINT && otherType == UnsignedType.ULONG -> "uintToULong(data)"
                    otherType > type -> "${otherType.capitalized}(data.to${otherType.asSigned.capitalized}() and ${type.mask})"
                    otherType == type -> "this"
                    else -> "data.to${otherType.capitalized}()"
                }
                body.setAsExpressionBody()
            }
        }
    }

    private fun ClassBuilder.generateFloatingConversions() {
        for (otherType in PrimitiveType.floatingPoint) {
            val otherName = otherType.capitalized
            val doc = buildList {
                add("Converts this [$className] value to [$otherName].")
                add("")
                if (type == UnsignedType.ULONG || type == UnsignedType.UINT && otherType == PrimitiveType.FLOAT) {
                    add("The resulting value is the closest `$otherName` to this `$className` value.")
                    add("In case when this `$className` value is exactly between two `$otherName`s,")
                    add("the one with zero at least significant bit of mantissa is selected.")
                } else {
                    add("The resulting `$otherName` value represents the same numerical value as this `$className`.")
                }
            }.joinToString(END_LINE)

            method {
                appendDoc(doc)
                annotations += INLINE_ONLY
                annotations += INTRINSIC_CONST_EVALUATION
                signature {
                    isInline = true
                    methodName = "to$otherName"
                    returnType = otherName
                }
                val body = when (type) {
                    UnsignedType.UINT, UnsignedType.ULONG -> className.lowercase() + "To$otherName(data)"
                    else -> "uintTo$otherName(this.toInt())"
                }
                body.setAsExpressionBody()
            }
        }
    }

    private fun FileBuilder.generateExtensionConversions() {
        for (otherType in UnsignedType.entries) {
            val otherSigned = otherType.asSigned.capitalized
            val thisSigned = type.asSigned.capitalized
            val doc = buildList {
                add("Converts this [$otherSigned] value to [$className].")
                add("")
                when {
                    otherType < type -> {
                        add("If this value is positive, the resulting `$className` value represents the same numerical value as this `$otherSigned`.")
                        add("")
                        add("The ${lsb(otherType.bitSize)} of the resulting `$className` value are the same as the bits of this `$otherSigned` value,")
                        add("whereas the ${msb(type.bitSize - otherType.bitSize)} are filled with the sign bit of this value.")
                    }
                    otherType == type -> {
                        add("If this value is positive, the resulting `$className` value represents the same numerical value as this `$otherSigned`.")
                        add("")
                        add("The resulting `$className` value has the same binary representation as this `$otherSigned` value.")
                    }
                    else -> {
                        add("If this value is positive and less than or equals to [$className.MAX_VALUE], the resulting `$className` value represents")
                        add("the same numerical value as this `$otherSigned`.")
                        add("")
                        add("The resulting `$className` value is represented by the ${lsb(type.bitSize)} of this `$otherSigned` value.")
                    }
                }
            }.joinToString(END_LINE)

            method {
                appendDoc(doc)
                annotations += "SinceKotlin(\"1.5\")"
                annotations += INLINE_ONLY
                annotations += INTRINSIC_CONST_EVALUATION
                signature {
                    isInline = true
                    extensionReceiver = otherSigned
                    methodName = "to$className"
                    returnType = className
                }
                val body = when {
                    otherType == type -> "$className(this)"
                    else -> "$className(this.to$thisSigned())"
                }
                body.setAsExpressionBody()
            }
        }

        if (type == UnsignedType.UBYTE || type == UnsignedType.USHORT)
            return // conversion from UByte/UShort to Float/Double is not allowed

        for (otherType in PrimitiveType.floatingPoint) {
            val otherName = otherType.capitalized
            method {
                appendDoc(
                    """
                    Converts this [$otherName] value to [$className].

                    The fractional part, if any, is rounded down towards zero.
                    Returns zero if this `$otherName` value is negative or `NaN`, [$className.MAX_VALUE] if it's bigger than `$className.MAX_VALUE`.
                    """.trimIndent()
                )
                annotations += "SinceKotlin(\"1.5\")"
                annotations += INLINE_ONLY
                annotations += INTRINSIC_CONST_EVALUATION
                signature {
                    isInline = true
                    extensionReceiver = otherName
                    methodName = "to$className"
                    returnType = className
                }
                "${otherName.lowercase()}To$className(this)".setAsExpressionBody()
            }
        }
    }

    private fun ClassBuilder.generateToStringHashCode() {
        method {
            annotations += INTRINSIC_CONST_EVALUATION
            signature {
                isOverride = true
                methodName = "toString"
                returnType = "String"
            }
            val body = when (type) {
                UnsignedType.UBYTE, UnsignedType.USHORT -> "toInt().toString()"
                UnsignedType.UINT -> "uintToString(data)"
                UnsignedType.ULONG -> "ulongToString(data)"
            }
            body.setAsExpressionBody()
        }
    }

    private fun maxByDomainCapacity(type1: UnsignedType, type2: UnsignedType): UnsignedType =
        if (type1.ordinal > type2.ordinal) type1 else type2

    private fun getOperatorReturnType(type1: UnsignedType, type2: UnsignedType): UnsignedType {
        return maxByDomainCapacity(maxByDomainCapacity(type1, type2), UnsignedType.UINT)
    }
}


class UnsignedArrayGenerator(val type: UnsignedType, val target: Target, private val out: PrintWriter) : BuiltInsGenerator {
    private val elementType = type.capitalized
    private val arrayType = elementType + "Array"
    private val arrayTypeOf = elementType.lowercase() + "ArrayOf"
    private val storageElementType = type.asSigned.capitalized
    private val storageArrayType = storageElementType + "Array"

    override fun generate() {
        out.print(buildFile().build())
    }

    private fun buildFile(): FileBuilder = file(this::class) {
        import("kotlin.jvm.*")

        klass {
            annotations += "SinceKotlin(\"1.3\")"
            annotations += "ExperimentalUnsignedTypes"
            annotations += "JvmInline"
            name = arrayType
            modifier(ClassModifier.VALUE)
            superType("Collection<$elementType>")
            primaryConstructor {
                visibility = MethodVisibility.INTERNAL
                annotations += PUBLISHED_API
                propertyParameter {
                    annotations += PUBLISHED_API
                    visibility = MethodVisibility.INTERNAL
                    name = "storage"
                    type = storageArrayType
                }
            }
            secondaryConstructor {
                visibility = MethodVisibility.PUBLIC
                appendDoc("Creates a new array of the specified [size], with all elements initialized to zero.")
                parameter { name = "size"; type = "Int" }
                primaryConstructorCall("$storageArrayType(size)")
            }

            method {
                appendDoc(
                    """
                    Returns the array element at the given [index]. This method can be called using the index operator.

                    If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS
                    where the behavior is unspecified.
                    """.trimIndent()
                )
                signature {
                    isOperator = true
                    methodName = "get"
                    parameter { name = "index"; type = "Int" }
                    returnType = elementType
                }
                "storage[index].to$elementType()".setAsExpressionBody()
            }

            method {
                appendDoc(
                    """
                    Sets the element at the given [index] to the given [value]. This method can be called using the index operator.

                    If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS
                    where the behavior is unspecified.
                    """.trimIndent()
                )
                signature {
                    isOperator = true
                    returnType = "Unit"
                    methodName = "set"
                    parameter { name = "index"; type = "Int" }
                    parameter { name = "value"; type = elementType }
                }
                "storage[index] = value.to$storageElementType()".setAsBlockBody()
            }

            this.property {
                appendDoc("Returns the number of elements in the array.")
                modifier("override")
                name = "size"
                type = "Int"
                "storage.size".setAsExpressionGetterBody()
            }

            method {
                appendDoc("Creates an iterator over the elements of the array.")
                signature {
                    isOverride = true
                    isOperator = true
                    methodName = "iterator"
                    returnType = "kotlin.collections.Iterator<$elementType>"
                }
                "Iterator(storage)".setAsExpressionBody()
            }

            klass {
                visibility = MethodVisibility.PRIVATE
                name = "Iterator"
                superType("kotlin.collections.Iterator<$elementType>")
                primaryConstructor {
                    visibility = null
                    propertyParameter {
                        visibility = MethodVisibility.PRIVATE
                        name = "array"
                        type = storageArrayType
                    }
                }
                property {
                    visibility = MethodVisibility.PRIVATE
                    isMutable = true
                    name = "index"
                    type = "Int"
                    value = "0"
                }
                method {
                    signature {
                        isOverride = true
                        returnType = "Boolean"
                        methodName = "hasNext"
                    }
                    "index < array.size".setAsExpressionBody()
                }
                method {
                    signature {
                        isOverride = true
                        returnType = elementType
                        methodName = "next"
                    }
                    "if (index < array.size) array[index++].to$elementType() else throw NoSuchElementException(index.toString())".setAsExpressionBody()
                }
            }

            method {
                signature {
                    isOverride = true
                    visibility = null
                    methodName = "contains"
                    parameter { name = "element"; type = elementType }
                    returnType = "Boolean"
                }
                "return storage.contains(element.to$storageElementType())".setAsBlockBody()
            }

            method {
                signature {
                    isOverride = true
                    visibility = null
                    methodName = "containsAll"
                    parameter { name = "elements"; type = "Collection<$elementType>" }
                    returnType = "Boolean"
                }
                "return (elements as Collection<*>).all { it is $elementType && storage.contains(it.to$storageElementType()) }".setAsBlockBody()
            }

            method {
                signature {
                    isOverride = true
                    visibility = null
                    methodName = "isEmpty"
                    returnType = "Boolean"
                }
                "this.storage.size == 0".setAsExpressionBody()
            }
        }

        method {
            appendDoc(
                """
                Creates a new array of the specified [size], where each element is calculated by calling the specified
                [init] function.

                The function [init] is called for each array element sequentially starting from the first one.
                It should return the value for an array element given its index.
                """.trimIndent()
            )
            annotations += "SinceKotlin(\"1.3\")"
            annotations += "ExperimentalUnsignedTypes"
            annotations += INLINE_ONLY
            signature {
                isInline = true
                methodName = arrayType
                parameter { name = "size"; type = "Int" }
                parameter { name = "init"; type = "(Int) -> $elementType" }
                returnType = arrayType
            }
            "return $arrayType($storageArrayType(size) { index -> init(index).to$storageElementType() })".setAsBlockBody()
        }

        method {
            annotations += "SinceKotlin(\"1.3\")"
            annotations += "ExperimentalUnsignedTypes"
            annotations += INLINE_ONLY
            signature {
                isInline = true
                methodName = arrayTypeOf
                parameter { isVararg = true; name = "elements"; type = elementType }
                returnType = arrayType
            }
            "elements".setAsExpressionBody()
        }
    }
}

class UnsignedRangeGenerator(val type: UnsignedType, val target: Target, private val out: PrintWriter) : BuiltInsGenerator {
    private val elementType = type.capitalized
    private val signedType = type.asSigned.capitalized
    private val stepType = signedType
    private val stepMinValue = "$stepType.MIN_VALUE"

    private fun hashCodeConversion(name: String, isSigned: Boolean = false) =
        if (type == UnsignedType.ULONG) "($name xor ($name ${if (isSigned) "u" else ""}shr 32))" else name

    override fun generate() {
        out.print(buildFile().build())
    }

    private fun buildFile(): FileBuilder = file(this::class, packageName = "kotlin.ranges") {
        import("kotlin.internal.*")

        generateRange()
        generateProgression()
        generateProgressionIterator()
    }

    private fun FileBuilder.generateRange() {
        klass {
            appendDoc("A range of values of type `$elementType`.")
            annotations += "SinceKotlin(\"1.5\")"
            name = "${elementType}Range"
            primaryConstructor {
                visibility = null
                parameter { name = "start"; type = elementType }
                parameter { name = "endInclusive"; type = elementType }
            }
            superType("${elementType}Progression(start, endInclusive, 1)")
            superType("ClosedRange<$elementType>")
            superType("OpenEndRange<$elementType>")

            property {
                visibility = null
                modifier("override")
                name = "start"
                type = elementType
                "first".setAsExpressionGetterBody()
            }
            property {
                visibility = null
                modifier("override")
                name = "endInclusive"
                type = elementType
                "last".setAsExpressionGetterBody()
            }
            property {
                visibility = null
                modifier("override")
                annotations += "Deprecated(\"Can throw an exception when it's impossible to represent the value with $elementType type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.\")"
                annotations += "SinceKotlin(\"1.9\")"
                annotations += "WasExperimental(ExperimentalStdlibApi::class)"
                name = "endExclusive"
                type = elementType
                """
                        if (last == $elementType.MAX_VALUE) error("Cannot return the exclusive upper bound of a range that includes MAX_VALUE.")
                        return last + 1u
                        """.trimIndent().setAsBlockGetterBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "contains"
                    parameter { name = "value"; type = elementType }
                    returnType = "Boolean"
                }
                "first <= value && value <= last".setAsExpressionBody()
            }

            method {
                appendDoc(
                    """
                    Checks if the range is empty.

                    The range is empty if its start value is greater than the end value.
                    """.trimIndent()
                )
                signature {
                    isOverride = true
                    methodName = "isEmpty"
                    returnType = "Boolean"
                }
                "first > last".setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "equals"
                    parameter { name = "other"; type = "Any?" }
                    returnType = "Boolean"
                }
                ("other is ${elementType}Range && (isEmpty() && other.isEmpty() ||" + END_LINE +
                        "        first == other.first && last == other.last)").setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "hashCode"
                    returnType = "Int"
                }
                "if (isEmpty()) -1 else (31 * ${hashCodeConversion("first")}.toInt() + ${hashCodeConversion("last")}.toInt())".setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "toString"
                    returnType = "String"
                }
                "\"\$first..\$last\"".setAsExpressionBody()
            }

            companionObject {
                property {
                    appendDoc("An empty range of values of type $elementType.")
                    name = "EMPTY"
                    type = "${elementType}Range"
                    value = "${elementType}Range($elementType.MAX_VALUE, $elementType.MIN_VALUE)"
                }
            }
        }
    }

    private fun FileBuilder.generateProgression() {
        klass {
            appendDoc("A progression of values of type `$elementType`.")
            annotations += "SinceKotlin(\"1.5\")"
            annotations += "Suppress(\"REDUNDANT_CALL_OF_CONVERSION_METHOD\")"
            name = "${elementType}Progression"
            modifier(ClassModifier.OPEN)
            primaryConstructor {
                visibility = MethodVisibility.INTERNAL
                parameter { name = "start"; type = elementType }
                parameter { name = "endInclusive"; type = elementType }
                parameter { name = "step"; type = stepType }
            }
            superType("Iterable<$elementType>")
            initBlock(
                """
                if (step == 0.to$stepType()) throw kotlin.IllegalArgumentException("Step must be non-zero.")
                if (step == $stepMinValue) throw kotlin.IllegalArgumentException("Step must be greater than $stepMinValue to avoid overflow on negation.")
                """.trimIndent()
            )

            property {
                appendDoc("The first element in the progression.")
                name = "first"
                type = elementType
                value = "start"
            }
            property {
                appendDoc("The last element in the progression.")
                name = "last"
                type = elementType
                value = "getProgressionLastElement(start, endInclusive, step)"
            }
            property {
                appendDoc("The step of the progression.")
                name = "step"
                type = stepType
                value = "step"
            }

            method {
                signature {
                    isFinal = true
                    isOverride = true
                    methodName = "iterator"
                    returnType = "Iterator<$elementType>"
                }
                "${elementType}ProgressionIterator(first, last, step)".setAsExpressionBody()
            }

            method {
                appendDoc(
                    """
                    Checks if the progression is empty.

                    Progression with a positive step is empty if its first element is greater than the last element.
                    Progression with a negative step is empty if its first element is less than the last element.
                    """.trimIndent()
                )
                signature {
                    isOpen = true
                    methodName = "isEmpty"
                    returnType = "Boolean"
                }
                "if (step > 0) first > last else first < last".setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "equals"
                    parameter { name = "other"; type = "Any?" }
                    returnType = "Boolean"
                }
                ("other is ${elementType}Progression && (isEmpty() && other.isEmpty() ||" + END_LINE +
                        "        first == other.first && last == other.last && step == other.step)").setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "hashCode"
                    returnType = "Int"
                }
                ("if (isEmpty()) -1 else (31 * (31 * ${hashCodeConversion("first")}.toInt() + ${hashCodeConversion("last")}.toInt()) + " +
                        "${hashCodeConversion("step", isSigned = true)}.toInt())").setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "toString"
                    returnType = "String"
                }
                "if (step > 0) \"\$first..\$last step \$step\" else \"\$first downTo \$last step \${-step}\"".setAsExpressionBody()
            }

            companionObject {
                method {
                    appendDoc(
                        """
                        Creates ${elementType}Progression within the specified bounds of a closed range.

                        The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
                        In order to go backwards the [step] must be negative.

                        [step] must be greater than `$stepMinValue` and not equal to zero.
                        """.trimIndent()
                    )
                    signature {
                        methodName = "fromClosedRange"
                        parameter { name = "rangeStart"; type = elementType }
                        parameter { name = "rangeEnd"; type = elementType }
                        parameter { name = "step"; type = stepType }
                        returnType = "${elementType}Progression"
                    }
                    "${elementType}Progression(rangeStart, rangeEnd, step)".setAsExpressionBody()
                }
            }
        }
    }

    private fun FileBuilder.generateProgressionIterator() {
        klass {
            appendDoc(
                """
                An iterator over a progression of values of type `$elementType`.
                @property step the number by which the value is incremented on each step.
                """.trimIndent()
            )
            annotations += "SinceKotlin(\"1.3\")"
            visibility = MethodVisibility.PRIVATE
            name = "${elementType}ProgressionIterator"
            primaryConstructor {
                visibility = null
                parameter { name = "first"; type = elementType }
                parameter { name = "last"; type = elementType }
                parameter { name = "step"; type = stepType }
            }
            superType("Iterator<$elementType>")

            property {
                visibility = MethodVisibility.PRIVATE
                name = "finalElement"
                type = elementType
                value = "last"
            }
            property {
                visibility = MethodVisibility.PRIVATE
                isMutable = true
                name = "hasNext"
                type = "Boolean"
                value = "if (step > 0) first <= last else first >= last"
            }
            property {
                visibility = MethodVisibility.PRIVATE
                additionalComments = "use 2-complement math for negative steps"
                name = "step"
                type = elementType
                value = "step.to$elementType()"
            }
            property {
                visibility = MethodVisibility.PRIVATE
                isMutable = true
                name = "next"
                type = elementType
                value = "if (hasNext) first else finalElement"
            }

            method {
                signature {
                    isOverride = true
                    methodName = "hasNext"
                    returnType = "Boolean"
                }
                "hasNext".setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "next"
                    returnType = elementType
                }
                """
                val value = next
                if (value == finalElement) {
                    if (!hasNext) throw kotlin.NoSuchElementException()
                    hasNext = false
                } else {
                    next += step
                }
                return value
                """.trimIndent().setAsBlockBody()
            }
        }
    }
}

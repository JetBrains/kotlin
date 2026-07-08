/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package unsigned.types

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.convert
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.BasePrimitivesGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ClassBuilder
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ClassModifier
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.END_LINE
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ExpectActualModifier
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.FileBuilder
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.MethodBuilder
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.MethodVisibility
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.file
import org.jetbrains.kotlin.generators.builtins.unsigned.INLINE_ONLY
import org.jetbrains.kotlin.generators.builtins.unsigned.INTRINSIC_CONST_EVALUATION
import org.jetbrains.kotlin.generators.builtins.unsigned.OVERRIDE_BY_INLINE
import org.jetbrains.kotlin.generators.builtins.unsigned.PUBLISHED_API
import java.io.PrintWriter
import kotlin.collections.iterator

abstract class BaseUnsignedTypeGenerator(
    val type: UnsignedType,
    private val out: PrintWriter,
    private val expectActualModifier: ExpectActualModifier,
) : BuiltInsGenerator {
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
        else -> BasePrimitivesGenerator.Companion.binaryOperatorDoc(operator, operand1.asSigned, operand2.asSigned)
    }

    override fun generate() {
        out.print(buildFile().build())
    }

    open val MIN_VALUE: String = "$className(0)"
    open val MAX_VALUE: String = "$className(-1)"
    open val SIZE_BYTES: String = "${type.byteSize}"
    open val SIZE_BITS: String = "${type.byteSize * 8}"

    open val extraImports = emptySet<String>()
    open val extraClassAnnotations = emptySet<String>()

    open fun compareToBody(otherType: UnsignedType): String = when {
        otherType == type && maxByDomainCapacity(type, UnsignedType.UINT) == type ->
            "${className.lowercase()}Compare(this.data, other.data)"

        maxOf(type, otherType) < UnsignedType.UINT ->
            "this.toInt().compareTo(other.toInt())"

        else -> {
            val ctype = maxByDomainCapacity(type, otherType)
            "${convert("this", type, ctype)}.compareTo(${convert("other", otherType, ctype)})"
        }
    }

    open fun binaryOperatorsBody(operator: String, otherType: UnsignedType, returnType: UnsignedType): String =
        if (type == otherType && type == returnType) {
            when (operator) {
                "plus", "minus", "times" -> "$className(this.data.$operator(other.data))"
                "div" -> when (type) {
                    UnsignedType.UINT -> "uintDivide(this, other)"
                    else -> "ulongDivide(this, other)"
                }
                "rem" -> when (type) {
                    UnsignedType.UINT -> "uintRemainder(this, other)"
                    else -> "ulongRemainder(this, other)"
                }
                else -> error(operator)
            }
        } else {
            "${convert("this", type, returnType)}.$operator(${convert("other", otherType, returnType)})"
        }

    open fun floorDivModeBody(function: String, otherType: UnsignedType, operationType: UnsignedType, returnType: UnsignedType): String =
        if (type == otherType && type == operationType) {
            when (function) {
                "floorDiv" -> "div(other)"
                "mod" -> "rem(other)"
                else -> error(function)
            }
        } else {
            convert(
                "${convert("this", type, operationType)}.$function(${convert("other", otherType, operationType)})",
                operationType, returnType
            )
        }

    open fun unaryOperatorBody(operator: String): String =
        "$className(data.$operator())"

    open fun rangeToBody(rangeType: String, thisRangeElement: String, otherRangeElement: String) =
        "$rangeType($thisRangeElement, $otherRangeElement)"

    open fun rangeUntilBody(rangeType: String, thisRangeElement: String, otherRangeElement: String) =
        "$thisRangeElement until $otherRangeElement"

    open fun bitShiftBody(operator: String): String =
        "$className(data $operator bitCount)"

    open fun bitwiseOperatorsBody(operator: String): String =
        "$className(this.data $operator other.data)"

    open fun inversionBody(): String =
        "$className(data.inv())"

    open fun signedConversionBody(otherType: UnsignedType): String {
        val signed = otherType.asSigned.capitalized
        return when {
            otherType < type -> "data.to$signed()"
            otherType == type -> "data"
            else -> "data.to$signed() and ${type.mask}"
        }
    }

    open fun unsignedConversionBody(otherType: UnsignedType): String =
        when {
            otherType > type -> "${otherType.capitalized}(data.to${otherType.asSigned.capitalized}() and ${type.mask})"
            otherType == type -> "this"
            else -> "data.to${otherType.capitalized}()"
        }

    open fun floatingConversionBody(otherType: PrimitiveType): String = when (otherType) {
        PrimitiveType.FLOAT -> "this.toDouble().toFloat()"
        PrimitiveType.DOUBLE -> when (type) {
            UnsignedType.UINT, UnsignedType.ULONG ->
                className.lowercase() + "To${otherType.capitalized}(data)"
            else -> "this.toUInt().toDouble()"
        }
        else -> error("Unexpected primitive type: $otherType")
    }

    open fun toStringHashCodeBody(): String = when (type) {
        UnsignedType.UBYTE, UnsignedType.USHORT -> "toInt().toString()"
        UnsignedType.UINT -> "toLong().toString()"
        UnsignedType.ULONG -> "ulongToString(data)"
    }

    open fun toStringWithBase(): String = when (type) {
        UnsignedType.UBYTE, UnsignedType.USHORT -> "toInt().toString(radix)"
        UnsignedType.UINT -> "toLong().toString(radix)"
        UnsignedType.ULONG -> "ulongToString(this.toLong(), checkRadix(radix))"
    }

    open fun toOtherUnsignedTypeBody(otherType: UnsignedType): String = when (type) {
        otherType -> "$className(this)"
        else -> "$className(this.to${type.asSigned.capitalized}())"
    }

    open fun fromFloatingPointBody(otherType: PrimitiveType): String = when (otherType) {
        PrimitiveType.FLOAT -> "this.toDouble().to${type.capitalized}()"
        PrimitiveType.DOUBLE -> when (type) {
            UnsignedType.UINT, UnsignedType.ULONG ->
                "${otherType.capitalized.lowercase()}To$className(this)"
            else -> error("Unexpected unsigned type: $className")
        }
        else -> error("Unexpected primitive type: $otherType")
    }

    private fun buildFile(): FileBuilder = file(this::class) {
        import("kotlin.experimental.*")
        extraImports.forEach(::import)

        klass {
            annotations += "SinceKotlin(\"1.5\")"
            annotations += extraClassAnnotations
            name = className
            expectActual = expectActualModifier
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
            generateToStringWithBase()
        }

        generateExtensionConversions()
    }

    private fun ClassBuilder.generateCompanionObject() {
        companionObject {
            property {
                appendDoc("A constant holding the minimum value an instance of $className can have.")
                name = "MIN_VALUE"
                type = className
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    modifier("const")
                    value = MIN_VALUE
                }
            }
            property {
                appendDoc("A constant holding the maximum value an instance of $className can have.")
                name = "MAX_VALUE"
                type = className

                if (expectActualModifier != ExpectActualModifier.Expect) {
                    modifier("const")
                    value = MAX_VALUE
                }
            }
            property {
                appendDoc("The number of bytes used to represent an instance of $className in a binary form.")
                name = "SIZE_BYTES"
                type = "Int"

                if (expectActualModifier != ExpectActualModifier.Expect) {
                    modifier("const")
                    value = SIZE_BYTES
                }
            }
            property {
                appendDoc("The number of bits used to represent an instance of $className in a binary form.")
                name = "SIZE_BITS"
                type = "Int"

                if (expectActualModifier != ExpectActualModifier.Expect) {
                    modifier("const")
                    value = SIZE_BITS
                }
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
                if (otherType == type) annotations += OVERRIDE_BY_INLINE
                signature {
                    isInline = true
                    isOperator = true
                    isOverride = otherType == type
                    methodName = "compareTo"
                    parameter { name = "other"; type = otherType.capitalized }
                    returnType = "Int"
                }

                if (expectActualModifier != ExpectActualModifier.Expect) {
                    compareToBody(otherType).setAsBody()
                }
            }
        }
    }

    private fun ClassBuilder.generateBinaryOperators() {
        for (name in BasePrimitivesGenerator.Companion.binaryOperators) {
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    binaryOperatorsBody(name, otherType, opReturnType).setAsBody()
                }
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    floorDivModeBody(name, otherType, operationType, opReturnType).setAsBody()
                }
            }
        }
    }

    private fun ClassBuilder.generateUnaryOperators() {
        for (name in listOf("inc", "dec")) {
            method {
                appendDoc(BasePrimitivesGenerator.Companion.incDecOperatorsDoc(name))
                annotations += INLINE_ONLY
                signature {
                    isInline = true
                    isOperator = true
                    methodName = name
                    returnType = className
                }
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    unaryOperatorBody(name).setAsBody()
                }
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
            if (expectActualModifier != ExpectActualModifier.Expect) {
                rangeToBody(rangeType, convertToRangeElement("this"), convertToRangeElement("other")).setAsBody()
            }
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
            if (expectActualModifier != ExpectActualModifier.Expect) {
                rangeUntilBody(rangeType, convertToRangeElement("this"), convertToRangeElement("other")).setAsBody()
            }
        }
    }

    private fun ClassBuilder.generateBitShiftOperators() {
        fun generateShiftOperator(name: String, implementation: String = name) {
            val doc = BasePrimitivesGenerator.Companion.shiftOperators[implementation]!!
            val detail = BasePrimitivesGenerator.Companion.shiftOperatorsDocDetail(type.asSigned)
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    bitShiftBody(implementation).setAsBody()
                }
            }
        }

        generateShiftOperator("shl")
        generateShiftOperator("shr", "ushr")
    }

    private fun ClassBuilder.generateBitwiseOperators() {
        for (entry in BasePrimitivesGenerator.Companion.bitwiseOperators) {
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    bitwiseOperatorsBody(name).setAsBody()
                }
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
            if (expectActualModifier != ExpectActualModifier.Expect) {
                inversionBody().setAsBody()
            }
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    signedConversionBody(otherType).setAsBody()
                }
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    unsignedConversionBody(otherType).setAsBody()
                }
            }
        }
    }

    private fun ClassBuilder.generateFloatingConversions() {
        for (otherType in PrimitiveType.Companion.floatingPoint) {
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    floatingConversionBody(otherType).setAsBody()
                }
            }
        }
    }

    private fun FileBuilder.generateExtensionConversions() {
        for (otherType in UnsignedType.entries) {
            val otherSigned = otherType.asSigned.capitalized
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
                expectActual = expectActualModifier
                signature {
                    isInline = true
                    expectActual = expectActualModifier
                    extensionReceiver = otherSigned
                    methodName = "to$className"
                    returnType = className
                }
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    toOtherUnsignedTypeBody(otherType).setAsBody()
                }
            }
        }

        if (type == UnsignedType.UBYTE || type == UnsignedType.USHORT)
            return // conversion from UByte/UShort to Float/Double is not allowed

        for (otherType in PrimitiveType.Companion.floatingPoint) {
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
                expectActual = expectActualModifier
                signature {
                    isInline = true
                    extensionReceiver = otherName
                    methodName = "to$className"
                    returnType = className
                }
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    fromFloatingPointBody(otherType).setAsBody()
                }
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
            if (expectActualModifier != ExpectActualModifier.Expect) {
                toStringHashCodeBody().setAsBody()
            }
        }
    }

    private fun ClassBuilder.generateToStringWithBase() {
        method {
            annotations += INLINE_ONLY
            signature {
                isInline = true
                visibility = MethodVisibility.INTERNAL
                methodName = "toStringWithBase"
                parameter { name = "radix"; type = "Int" }
                returnType = "String"
            }
            if (expectActualModifier != ExpectActualModifier.Expect) {
                toStringWithBase().setAsBody()
            }
        }
    }

    protected fun maxByDomainCapacity(type1: UnsignedType, type2: UnsignedType): UnsignedType =
        if (type1.ordinal > type2.ordinal) type1 else type2

    protected fun getOperatorReturnType(type1: UnsignedType, type2: UnsignedType): UnsignedType {
        return maxByDomainCapacity(maxByDomainCapacity(type1, type2), UnsignedType.UINT)
    }
}

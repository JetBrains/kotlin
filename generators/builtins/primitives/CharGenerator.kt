/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.NativePrimitivesGenerator.Companion.setAsExternal
import java.io.PrintWriter

abstract class CharGenerator(private val writer: PrintWriter) : BuiltInsGenerator {
    override fun generate() {
        writer.print(generateFile().build())
    }

    private fun generateFile(): FileBuilder {
        return file(this::class) { generateClass() }.apply { this.modifyGeneratedFile() }
    }

    private fun FileBuilder.generateClass() {
        klass {
            appendDoc("Represents a 16-bit Unicode character.")
            expectActual = ExpectActualModifier.Actual
            name = PrimitiveType.CHAR.capitalized
            superType("Comparable<$name>")

            generateCompareTo()
            generatePlus()
            generateMinusChar()
            generateMinusInt()
            generateInc()
            generateDec()
            generateRangeTo()
            generateRangeUntil()
            generateConversions()

            generateToString()
            generateEquals()
            generateHashCode()
            generateAdditionalMethods()

            generateCompanionObject()
        }.modifyGeneratedClass()
    }

    private fun ClassBuilder.generateCompanionObject() {
        companionObject {
            property {
                appendDoc("The minimum value of a character code unit.")
                annotations += "SinceKotlin(\"1.3\")"
                name = "MIN_VALUE"
                type = PrimitiveType.CHAR.capitalized
                value = "'\\u0000'"
            }

            property {
                appendDoc("The maximum value of a character code unit.")
                annotations += "SinceKotlin(\"1.3\")"
                name = "MAX_VALUE"
                type = PrimitiveType.CHAR.capitalized
                value = "'\\uFFFF'"
            }

            property {
                appendDoc("The minimum value of a Unicode high-surrogate code unit.")
                name = "MIN_HIGH_SURROGATE"
                type = PrimitiveType.CHAR.capitalized
                value = "'\\uD800'"
            }

            property {
                appendDoc("The maximum value of a Unicode high-surrogate code unit.")
                name = "MAX_HIGH_SURROGATE"
                type = PrimitiveType.CHAR.capitalized
                value = "'\\uDBFF'"
            }

            property {
                appendDoc("The minimum value of a Unicode low-surrogate code unit.")
                name = "MIN_LOW_SURROGATE"
                type = PrimitiveType.CHAR.capitalized
                value = "'\\uDC00'"
            }

            property {
                appendDoc("The maximum value of a Unicode low-surrogate code unit.")
                name = "MAX_LOW_SURROGATE"
                type = PrimitiveType.CHAR.capitalized
                value = "'\\uDFFF'"
            }

            property {
                appendDoc("The minimum value of a Unicode surrogate code unit.")
                name = "MIN_SURROGATE"
                type = PrimitiveType.CHAR.capitalized
                value = "MIN_HIGH_SURROGATE"
            }

            property {
                appendDoc("The maximum value of a Unicode surrogate code unit.")
                name = "MAX_SURROGATE"
                type = PrimitiveType.CHAR.capitalized
                value = "MAX_LOW_SURROGATE"
            }

            property {
                appendDoc("The number of bytes used to represent a Char in a binary form.")
                annotations += "SinceKotlin(\"1.3\")"
                name = "SIZE_BYTES"
                type = PrimitiveType.INT.capitalized
                value = "2"
            }

            property {
                appendDoc("The number of bits used to represent a Char in a binary form.")
                annotations += "SinceKotlin(\"1.3\")"
                name = "SIZE_BITS"
                type = PrimitiveType.INT.capitalized
                value = "16"
            }
        }.modifyGeneratedCompanionObject()
    }

    private fun ClassBuilder.generateCompareTo() {
        val doc = """
            Compares this value with the specified value for order.
            
            Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
            or a positive number if it's greater than other.
        """.trimIndent()
        method {
            appendDoc(doc)
            annotations += intrinsicConstEvaluationAnnotation
            signature {
                isOverride = true
                methodName = "compareTo"
                parameter {
                    name = "other"
                    type = PrimitiveType.CHAR.capitalized
                }
                returnType = PrimitiveType.INT.capitalized
            }
        }.modifyGeneratedCompareTo()
    }

    private fun ClassBuilder.generatePlus() {
        method {
            appendDoc("Adds the other Int value to this value resulting a Char.")
            annotations += intrinsicConstEvaluationAnnotation
            signature {
                isOperator = true
                methodName = "plus"
                parameter {
                    name = "other"
                    type = PrimitiveType.INT.capitalized
                }
                returnType = PrimitiveType.CHAR.capitalized
            }
        }.modifyGeneratedPlus()
    }

    private fun ClassBuilder.generateMinusChar() {
        method {
            appendDoc("Subtracts the other Char value from this value resulting an Int.")
            annotations += intrinsicConstEvaluationAnnotation
            signature {
                isOperator = true
                methodName = "minus"
                parameter {
                    name = "other"
                    type = PrimitiveType.CHAR.capitalized
                }
                returnType = PrimitiveType.INT.capitalized
            }
        }.modifyGeneratedMinusChar()
    }

    private fun ClassBuilder.generateMinusInt() {
        method {
            appendDoc("Subtracts the other Int value from this value resulting a Char.")
            annotations += intrinsicConstEvaluationAnnotation
            signature {
                isOperator = true
                methodName = "minus"
                parameter {
                    name = "other"
                    type = PrimitiveType.INT.capitalized
                }
                returnType = PrimitiveType.CHAR.capitalized
            }
        }.modifyGeneratedMinusInt()
    }

    private fun ClassBuilder.generateInc() {
        val doc = """
            Returns this value incremented by one.
            
            @sample samples.misc.Builtins.inc
        """.trimIndent()
        method {
            appendDoc(doc)
            signature {
                isOperator = true
                methodName = "inc"
                returnType = PrimitiveType.CHAR.capitalized
            }
        }.modifyGeneratedInc()
    }

    private fun ClassBuilder.generateDec() {
        val doc = """
            Returns this value decremented by one.
            
            @sample samples.misc.Builtins.dec
        """.trimIndent()
        method {
            appendDoc(doc)
            signature {
                isOperator = true
                methodName = "dec"
                returnType = PrimitiveType.CHAR.capitalized
            }
        }.modifyGeneratedDec()
    }

    private fun ClassBuilder.generateRangeTo() {
        method {
            appendDoc("Creates a range from this value to the specified [other] value.")
            signature {
                isOperator = true
                methodName = "rangeTo"
                parameter {
                    name = "other"
                    type = PrimitiveType.CHAR.capitalized
                }
                returnType = "CharRange"
            }
        }.modifyGeneratedRangeTo()
    }

    private fun ClassBuilder.generateRangeUntil() {
        val doc = """
            Creates a range from this value up to but excluding the specified [other] value.
            
            If the [other] value is less than or equal to `this` value, then the returned range is empty.
        """.trimIndent()
        method {
            appendDoc(doc)
            annotations += "SinceKotlin(\"1.9\")"
            annotations += "WasExperimental(ExperimentalStdlibApi::class)"
            signature {
                isOperator = true
                methodName = "rangeUntil"
                parameter {
                    name = "other"
                    type = PrimitiveType.CHAR.capitalized
                }
                returnType = "CharRange"
            }
        }.modifyGeneratedRangeUntil()
    }

    private fun ClassBuilder.generateConversions() {
        for (otherKind in PrimitiveType.exceptBoolean) {
            val otherName = otherKind.capitalized
            method {
                appendDoc("Returns the value of this character as a `$otherName`.")
                if (otherKind != PrimitiveType.CHAR) {
                    val replaceWith = if (otherKind == PrimitiveType.INT) "this.code" else "this.code.to$otherName()"
                    annotations += "Deprecated(\"Conversion of Char to Number is deprecated. Use Char.code property instead.\", ReplaceWith(\"$replaceWith\"))"
                    annotations += "DeprecatedSinceKotlin(warningSince = \"1.5\")"
                }
                annotations += intrinsicConstEvaluationAnnotation
                signature {
                    methodName = "to$otherName"
                    returnType = otherKind.capitalized
                }
            }.modifyGeneratedConversions(otherKind)
        }
    }

    private fun ClassBuilder.generateToString() {
        method {
            annotations += intrinsicConstEvaluationAnnotation
            signature {
                methodName = "toString"
                isOverride = true
                returnType = "String"
            }
        }.modifyGeneratedToString()
    }

    private fun ClassBuilder.generateEquals() {
        method {
            annotations += intrinsicConstEvaluationAnnotation
            signature {
                isOverride = true
                methodName = "equals"
                parameter {
                    name = "other"
                    type = "Any?"
                }
                returnType = PrimitiveType.BOOLEAN.capitalized
            }
        }.modifyGeneratedEquals()
    }

    private fun ClassBuilder.generateHashCode() {
        method {
            signature {
                isOverride = true
                methodName = "hashCode"
                returnType = PrimitiveType.INT.capitalized
            }
        }.modifyGeneratedHashCode()
    }

    internal open fun FileBuilder.modifyGeneratedFile() {}
    internal open fun ClassBuilder.modifyGeneratedClass() {}
    internal open fun CompanionObjectBuilder.modifyGeneratedCompanionObject() {}
    internal open fun MethodBuilder.modifyGeneratedCompareTo() {}
    internal open fun MethodBuilder.modifyGeneratedPlus() {}
    internal open fun MethodBuilder.modifyGeneratedMinusChar() {}
    internal open fun MethodBuilder.modifyGeneratedMinusInt() {}
    internal open fun MethodBuilder.modifyGeneratedInc() {}
    internal open fun MethodBuilder.modifyGeneratedDec() {}
    internal open fun MethodBuilder.modifyGeneratedRangeTo() {}
    internal open fun MethodBuilder.modifyGeneratedRangeUntil() {}
    internal open fun MethodBuilder.modifyGeneratedConversions(otherKind: PrimitiveType) {}
    internal open fun MethodBuilder.modifyGeneratedToString() {}
    internal open fun MethodBuilder.modifyGeneratedEquals() {}
    internal open fun MethodBuilder.modifyGeneratedHashCode() {}
    internal open fun ClassBuilder.generateAdditionalMethods() {}
}

class CommonCharGenerator(writer: PrintWriter) : CharGenerator(writer) {
    override fun FileBuilder.modifyGeneratedFile() {
        import("kotlin.internal.ActualizeByJvmBuiltinProvider")
    }

    override fun ClassBuilder.modifyGeneratedClass() {
        expectActual = ExpectActualModifier.Expect
        annotations += "ActualizeByJvmBuiltinProvider"
    }

    override fun CompanionObjectBuilder.modifyGeneratedCompanionObject() {
        annotations += """Suppress("EXPECTED_PROPERTY_INITIALIZER")"""
    }
}

class JvmCharGenerator(writer: PrintWriter) : CharGenerator(writer) {
    override fun ClassBuilder.modifyGeneratedClass() {
        appendDoc("On the JVM, non-nullable values of this type are represented as values of the primitive type `char`.")
        expectActual = ExpectActualModifier.Unspecified
    }
}

class JsCharGenerator(writer: PrintWriter) : CharGenerator(writer) {
    override fun FileBuilder.modifyGeneratedFile() {
        appendFileComment("Char is a magic class.")
        appendFileComment("Char is defined as a regular class, but we lower it as a value class.")
        appendFileComment("See [org.jetbrains.kotlin.ir.backend.js.utils.JsInlineClassesUtils.isClassInlineLike] for explanation.")
    }

    override fun ClassBuilder.modifyGeneratedClass() {
        primaryConstructor {
            annotations += "kotlin.internal.LowPriorityInOverloadResolution"
            visibility = MethodVisibility.INTERNAL
            parameter {
                name = "private val value"
                type = PrimitiveType.INT.capitalized
            }
        }

        secondaryConstructor {
            annotations += "SinceKotlin(\"1.5\")"
            annotations += "WasExperimental(ExperimentalStdlibApi::class)"
            visibility = MethodVisibility.PUBLIC
            expectActual = ExpectActualModifier.Unspecified
            parameter {
                name = "code"
                type = "UShort"
            }
            argument("code.toInt()")
        }
    }

    override fun MethodBuilder.modifyGeneratedCompareTo() {
        "value - other.value".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedPlus() {
        "(value + other).toChar()".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedMinusChar() {
        "value - other.value".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedMinusInt() {
        "(value - other).toChar()".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedInc() {
        "(value + 1).toChar()".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedDec() {
        "(value - 1).toChar()".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedRangeTo() {
        "CharRange(this, other)".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedRangeUntil() {
        "this until other".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedConversions(otherKind: PrimitiveType) {
        val body = when (otherKind) {
            PrimitiveType.CHAR -> "this"
            PrimitiveType.INT -> "value"
            else -> "value.$methodName()"
        }
        body.setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedEquals() {
        """
            if (other !is Char) return false
            return this.value == other.value
        """.trimIndent().setAsBlockBody()
    }

    override fun MethodBuilder.modifyGeneratedToString() {
        additionalComments = "TODO implicit usages of toString and valueOf must be covered in DCE"
        annotations += "Suppress(\"JS_NAME_PROHIBITED_FOR_OVERRIDE\")"
        annotations += "JsName(\"toString\")"
        "return js(\"String\").fromCharCode(value).unsafeCast<String>()".setAsBlockBody()
    }

    override fun MethodBuilder.modifyGeneratedHashCode() {
        "value".setAsExpressionBody()
    }
}

class WasmCharGenerator(writer: PrintWriter) : CharGenerator(writer) {
    override fun FileBuilder.modifyGeneratedFile() {
        import("kotlin.wasm.internal.*")
    }

    override fun ClassBuilder.modifyGeneratedClass() {
        annotations += "WasmAutoboxed"
        annotations += "Suppress(\"NOTHING_TO_INLINE\")"
        primaryConstructor {
            parameter {
                name = "private val value"
                type = PrimitiveType.CHAR.capitalized
            }
        }
    }

    override fun CompanionObjectBuilder.modifyGeneratedCompanionObject() {
        property {
            appendDoc("The minimum value of a supplementary code point, `\\u0x10000`.")
            visibility = MethodVisibility.INTERNAL
            expectActual = ExpectActualModifier.Unspecified
            name = "MIN_SUPPLEMENTARY_CODE_POINT"
            type = PrimitiveType.INT.capitalized
            value = "0x10000"
        }

        property {
            appendDoc("The minimum value of a Unicode code point.")
            visibility = MethodVisibility.INTERNAL
            expectActual = ExpectActualModifier.Unspecified
            name = "MIN_CODE_POINT"
            type = PrimitiveType.INT.capitalized
            value = "0x000000"
        }

        property {
            appendDoc("The maximum value of a Unicode code point.")
            visibility = MethodVisibility.INTERNAL
            expectActual = ExpectActualModifier.Unspecified
            name = "MAX_CODE_POINT"
            type = PrimitiveType.INT.capitalized
            value = "0X10FFFF"
        }

        property {
            appendDoc("The minimum radix available for conversion to and from strings.")
            visibility = MethodVisibility.INTERNAL
            expectActual = ExpectActualModifier.Unspecified
            name = "MIN_RADIX"
            type = PrimitiveType.INT.capitalized
            value = "2"
        }

        property {
            appendDoc("The maximum radix available for conversion to and from strings.")
            visibility = MethodVisibility.INTERNAL
            expectActual = ExpectActualModifier.Unspecified
            name = "MAX_RADIX"
            type = PrimitiveType.INT.capitalized
            value = "36"
        }
    }

    override fun MethodBuilder.modifyGeneratedCompareTo() {
        "wasm_i32_compareTo(this.code, other.code)".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedPlus() {
        modifySignature { isInline = true }
        "(this.code + other).toChar()".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedMinusChar() {
        modifySignature { isInline = true }
        "(this.code - other.code)".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedMinusInt() {
        modifySignature { isInline = true }
        "(this.code - other).toChar()".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedInc() {
        modifySignature { isInline = true }
        "(this.code + 1).toChar()".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedDec() {
        modifySignature { isInline = true }
        "(this.code - 1).toChar()".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedRangeTo() {
        "CharRange(this, other)".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedRangeUntil() {
        "this until other".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedConversions(otherKind: PrimitiveType) {
        modifySignature { isInline = methodName != "toInt" }
        val body = when (otherKind) {
            PrimitiveType.CHAR -> "this"
            PrimitiveType.INT -> {
                annotations += "WasmNoOpCast"
                "implementedAsIntrinsic"
            }
            else -> "this.code.$methodName()"
        }
        body.setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedEquals() {
        """
            if (other is Char)
                return wasm_i32_eq(this.code, other.code)
            return false
        """.trimIndent().setAsBlockBody()
    }

    override fun MethodBuilder.modifyGeneratedToString() {
        """
            val array = WasmCharArray(1)
            array.set(0, this)
            return array.createString()
        """.trimIndent().setAsBlockBody()
    }

    override fun MethodBuilder.modifyGeneratedHashCode() {
        "this.code.hashCode()".setAsExpressionBody()
    }
}

class NativeCharGenerator(writer: PrintWriter) : CharGenerator(writer) {
    override fun FileBuilder.modifyGeneratedFile() {
        suppress("NOTHING_TO_INLINE")
        import("kotlin.experimental.ExperimentalNativeApi")
        import("kotlin.native.internal.GCUnsafeCall")
        import("kotlin.native.internal.TypedIntrinsic")
        import("kotlin.native.internal.IntrinsicType")
        import("kotlin.native.internal.escapeAnalysis.Escapes")
    }

    override fun CompanionObjectBuilder.modifyGeneratedCompanionObject() {
        annotations += "kotlin.native.internal.CanBePrecreated"
        property {
            appendDoc(
                """
                    The minimum value of a supplementary code point, `\u0x10000`.
                    
                    Note that this constant is experimental.
                    In the future it could be deprecated in favour of another constant of a `CodePoint` type.
                """.trimIndent()
            )
            annotations += "ExperimentalNativeApi"
            expectActual = ExpectActualModifier.Unspecified
            name = "MIN_SUPPLEMENTARY_CODE_POINT"
            type = PrimitiveType.INT.capitalized
            value = "0x10000"
        }

        property {
            appendDoc(
                """
                    The minimum value of a Unicode code point.
                    
                    Note that this constant is experimental.
                    In the future it could be deprecated in favour of another constant of a `CodePoint` type.
                """.trimIndent()
            )
            annotations += "ExperimentalNativeApi"
            expectActual = ExpectActualModifier.Unspecified
            name = "MIN_CODE_POINT"
            type = PrimitiveType.INT.capitalized
            value = "0x000000"
        }

        property {
            appendDoc(
                """
                    The maximum value of a Unicode code point.
                    
                    Note that this constant is experimental.
                    In the future it could be deprecated in favour of another constant of a `CodePoint` type.
                """.trimIndent()
            )
            annotations += "ExperimentalNativeApi"
            expectActual = ExpectActualModifier.Unspecified
            name = "MAX_CODE_POINT"
            type = PrimitiveType.INT.capitalized
            value = "0X10FFFF"
        }

        property {
            appendDoc("The minimum radix available for conversion to and from strings.")
            annotations += "Deprecated(\"Introduce your own constant with the value of `2`\", ReplaceWith(\"2\"))"
            annotations += "DeprecatedSinceKotlin(warningSince = \"1.9\", errorSince = \"2.1\")"
            expectActual = ExpectActualModifier.Unspecified
            name = "MIN_RADIX"
            type = PrimitiveType.INT.capitalized
            value = "2"
        }

        property {
            appendDoc("The maximum radix available for conversion to and from strings.")
            annotations += "Deprecated(\"Introduce your own constant with the value of `36\", ReplaceWith(\"36\"))"
            annotations += "DeprecatedSinceKotlin(warningSince = \"1.9\", errorSince = \"2.1\")"
            expectActual = ExpectActualModifier.Unspecified
            name = "MAX_RADIX"
            type = PrimitiveType.INT.capitalized
            value = "36"
        }
    }

    override fun MethodBuilder.modifyGeneratedCompareTo() {
        setAsExternal(PrimitiveType.CHAR)
    }

    override fun MethodBuilder.modifyGeneratedPlus() {
        modifySignature { isInline = true }
        "(this.code + other).toChar()".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedMinusChar() {
        modifySignature { isInline = true }
        "this.code - other.code".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedMinusInt() {
        modifySignature { isInline = true }
        "(this.code - other).toChar()".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedInc() {
        setAsExternal(PrimitiveType.CHAR)
    }

    override fun MethodBuilder.modifyGeneratedDec() {
        setAsExternal(PrimitiveType.CHAR)
    }

    override fun MethodBuilder.modifyGeneratedRangeTo() {
        "CharRange(this, other)".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedRangeUntil() {
        "this until other".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedConversions(otherKind: PrimitiveType) {
        modifySignature { isExternal = methodName != "toChar" }
        when (otherKind) {
            PrimitiveType.BYTE -> annotations += "TypedIntrinsic(IntrinsicType.INT_TRUNCATE)"
            PrimitiveType.CHAR -> {
                modifySignature { isInline = true }
                "this".setAsExpressionBody()
            }
            PrimitiveType.SHORT, PrimitiveType.INT, PrimitiveType.LONG -> annotations += "TypedIntrinsic(IntrinsicType.ZERO_EXTEND)"
            PrimitiveType.FLOAT, PrimitiveType.DOUBLE -> annotations += "TypedIntrinsic(IntrinsicType.UNSIGNED_TO_FLOAT)"
            else -> error("Unsupported `Char` conversion to $otherKind")
        }
    }

    override fun MethodBuilder.modifyGeneratedEquals() {
        "other is Char && this.code == other.code".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedToString() {
        annotations += "GCUnsafeCall(\"Kotlin_Char_toString\")"
        annotations += "Escapes.Nothing"
        modifySignature { isExternal = true }
    }

    private fun ClassBuilder.generateCustomEquals() {
        method {
            annotations += "Deprecated(\"Provided for binary compatibility\", level = DeprecationLevel.HIDDEN)"
            annotations += intrinsicConstEvaluationAnnotation
            expectActual = ExpectActualModifier.Unspecified
            signature {
                methodName = "equals"
                parameter {
                    name = "other"
                    type = PrimitiveType.CHAR.capitalized
                }
                returnType = PrimitiveType.BOOLEAN.capitalized
            }

            "this == other".setAsExpressionBody()
        }
    }

    override fun MethodBuilder.modifyGeneratedHashCode() {
        "return this.code".setAsBlockBody()
    }

    override fun ClassBuilder.generateAdditionalMethods() {
        generateCustomEquals()
    }
}

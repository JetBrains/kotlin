/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import java.io.PrintWriter
import java.util.*

class WasmPrimitivesGenerator(writer: PrintWriter) : BasePrimitivesGenerator(writer) {
    override fun FileDescription.modifyGeneratedFile() {
        this.addSuppress("OVERRIDE_BY_INLINE")
        this.addSuppress("NOTHING_TO_INLINE")
        this.addSuppress("unused")
        this.addSuppress("UNUSED_PARAMETER")
        this.addImport("kotlin.wasm.internal.*")
    }

    override fun ClassDescription.modifyGeneratedClass(thisKind: PrimitiveType) {
        addAnnotation("WasmAutoboxed")
        // used here little hack with name extension just to avoid creation of specialized "ConstructorParameterDescription"
        constructorArg = MethodParameter("private val value", thisKind.capitalized)
    }

    override fun CompanionObjectDescription.modifyGeneratedCompanionObject(thisKind: PrimitiveType) {
        isPublic = true
    }

    override fun primitiveConstants(type: PrimitiveType): List<Any> {
        return when (type) {
            PrimitiveType.FLOAT -> listOf(
                String.format(Locale.US, "%.17eF", java.lang.Float.MIN_VALUE),
                String.format(Locale.US, "%.17eF", java.lang.Float.MAX_VALUE),
                "1.0F/0.0F", "-1.0F/0.0F", "-(0.0F/0.0F)"
            )
            else -> super.primitiveConstants(type)
        }
    }

    override fun PropertyDescription.modifyGeneratedCompanionObjectProperty(thisKind: PrimitiveType) {
        if (this.name in setOf("POSITIVE_INFINITY", "NEGATIVE_INFINITY", "NaN")) {
            this.addAnnotation("Suppress(\"DIVISION_BY_ZERO\")")
        }
    }

    override fun MethodDescription.modifyGeneratedCompareTo(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        if (thisKind != otherKind || thisKind !in PrimitiveType.floatingPoint) {
            this.signature.isInline = true
        }

        val argName = this.signature.arg!!.name
        if (otherKind == thisKind) {
            if (thisKind in PrimitiveType.floatingPoint) {
                """
                    // if any of values in NaN both comparisons return false
                    if (this > $argName) return 1
                    if (this < $argName) return -1
            
                    val thisBits = this.toBits()
                    val otherBits = $argName.toBits()
            
                    // Canonical NaN bits representation higher than any other bit represent value
                    return thisBits.compareTo(otherBits)
                """.trimIndent().addAsMultiLineBody()
            } else {
                val body = when (thisKind) {
                    PrimitiveType.BYTE -> "wasm_i32_compareTo(this.toInt(), $argName.toInt())"
                    PrimitiveType.SHORT -> "this.toInt().compareTo($argName.toInt())"
                    PrimitiveType.INT, PrimitiveType.LONG -> "wasm_${thisKind.prefixLowercase}_compareTo(this, $argName)"
                    else -> throw IllegalArgumentException("Unsupported type $thisKind for generation `compareTo` method")
                }
                body.addAsSingleLineBody(bodyOnNewLine = true)
            }
            return
        }

        val thisCasted = "this" + thisKind.castToIfNecessary(otherKind)
        val otherCasted = argName + otherKind.castToIfNecessary(thisKind)
        when {
            thisKind == PrimitiveType.FLOAT && otherKind == PrimitiveType.DOUBLE -> "- ${otherCasted}.compareTo(this)"
            else -> "$thisCasted.compareTo($otherCasted)"
        }.addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodDescription.modifyGeneratedBinaryOperation(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        val sign = this.signature.name.asSign()
        val argName = this.signature.arg!!.name
        if (thisKind != PrimitiveType.BYTE && thisKind != PrimitiveType.SHORT && thisKind == otherKind) {
            val type = thisKind.capitalized

            when (val methodName = this.signature.name) {
                "div" -> {
                    val oneConst = if (thisKind == PrimitiveType.LONG) "-1L" else "-1"
                    when (thisKind) {
                        PrimitiveType.INT, PrimitiveType.LONG -> "if (this == $type.MIN_VALUE && $argName == $oneConst) $type.MIN_VALUE else wasm_${thisKind.prefixLowercase}_div_s(this, $argName)"
                        else -> return implementAsIntrinsic(thisKind, methodName)
                    }
                }
                "rem" -> when (thisKind) {
                    in PrimitiveType.floatingPoint -> "this - (wasm_${thisKind.prefixLowercase}_nearest(this / $argName) * $argName)"
                    else -> return implementAsIntrinsic(thisKind, methodName)
                }
                else -> return implementAsIntrinsic(thisKind, methodName)
            }.addAsSingleLineBody(bodyOnNewLine = true)
            return
        }

        this.signature.isInline = true
        val returnTypeAsPrimitive = PrimitiveType.valueOf(this.signature.returnType.uppercase())
        val thisCasted = "this" + thisKind.castToIfNecessary(returnTypeAsPrimitive)
        val otherCasted = argName + this.signature.arg.getTypeAsPrimitive().castToIfNecessary(returnTypeAsPrimitive)
        "$thisCasted $sign $otherCasted".addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodDescription.modifyGeneratedUnaryOperation(thisKind: PrimitiveType) {
        val methodName = this.signature.name
        if (thisKind == PrimitiveType.INT && methodName == "dec") {
            setAdditionalDoc("TODO: Fix test compiler/testData/codegen/box/functions/invoke/invoke.kt with inline dec")
        } else {
            this.signature.isInline = true
        }

        if (methodName in setOf("inc", "dec")) {
            val sign = if (methodName == "inc") "+" else "-"
            when (thisKind) {
                PrimitiveType.BYTE, PrimitiveType.SHORT -> "(this $sign 1).to${thisKind.capitalized}()".addAsSingleLineBody(bodyOnNewLine = true)
                PrimitiveType.INT -> "this $sign 1".addAsSingleLineBody(bodyOnNewLine = true)
                PrimitiveType.LONG -> "this $sign 1L".addAsSingleLineBody(bodyOnNewLine = true)
                PrimitiveType.FLOAT -> "this $sign 1.0f".addAsSingleLineBody(bodyOnNewLine = true)
                PrimitiveType.DOUBLE -> "this $sign 1.0".addAsSingleLineBody(bodyOnNewLine = true)
                else -> Unit
            }
        }

        if (methodName in setOf("unaryMinus", "unaryPlus")) {
            if (thisKind in PrimitiveType.floatingPoint && methodName == "unaryMinus") {
                return implementAsIntrinsic(thisKind, methodName)
            }

            val returnTypeAsPrimitive = PrimitiveType.valueOf(this.signature.returnType.uppercase())
            val thisCasted = "this" + thisKind.castToIfNecessary(returnTypeAsPrimitive)
            val sign = if (methodName == "unaryMinus") {
                when (thisKind) {
                    PrimitiveType.INT -> "0 - "
                    PrimitiveType.LONG -> "0L - "
                    else -> "-"
                }
            } else ""
            "$sign$thisCasted".addAsSingleLineBody(bodyOnNewLine = true)
        }
    }

    override fun MethodDescription.modifyGeneratedRangeTo(thisKind: PrimitiveType) {
        val rangeType = PrimitiveType.valueOf(this.signature.returnType.replace("Range", "").uppercase())
        val thisCasted = "this" + thisKind.castToIfNecessary(rangeType)
        val otherCasted = this.signature.arg!!.name + this.signature.arg.getTypeAsPrimitive().castToIfNecessary(rangeType)
        "return ${this.signature.returnType}($thisCasted, $otherCasted)".addAsMultiLineBody()
    }

    override fun MethodDescription.modifyGeneratedRangeUntil(thisKind: PrimitiveType) {
        "this until ${this.signature.arg!!.name}".addAsSingleLineBody(bodyOnNewLine = false)
    }

    override fun MethodDescription.modifyGeneratedBitShiftOperators(thisKind: PrimitiveType) {
        if (thisKind == PrimitiveType.INT) {
            implementAsIntrinsic(thisKind, this.signature.name)
        } else if (thisKind == PrimitiveType.LONG) {
            this.signature.isInline = true
            "wasm_i64_${this.signature.name.toWasmOperator().lowercase()}(this, ${signature.arg!!.name}.toLong())".addAsSingleLineBody(bodyOnNewLine = true)
        }
    }

    override fun MethodDescription.modifyGeneratedBitwiseOperators(thisKind: PrimitiveType) {
        if (this.signature.name == "inv") {
            this.signature.isInline = true
            val oneConst = if (thisKind == PrimitiveType.LONG) "-1L" else "-1"
            "this.xor($oneConst)".addAsSingleLineBody(bodyOnNewLine = true)
            return
        }

        implementAsIntrinsic(thisKind, this.signature.name)
    }

    override fun MethodDescription.modifyGeneratedConversions(thisKind: PrimitiveType) {
        val returnTypeAsPrimitive = PrimitiveType.valueOf(this.signature.returnType.uppercase())
        if (returnTypeAsPrimitive == thisKind) {
            this.signature.isInline = true
            "this".addAsSingleLineBody(bodyOnNewLine = true)
            return
        }

        when (thisKind) {
            PrimitiveType.BYTE, PrimitiveType.SHORT -> when (returnTypeAsPrimitive) {
                // byte to byte conversion impossible here due to earlier check on type equality
                PrimitiveType.BYTE -> "this.toInt().toByte()".also { this.signature.isInline = true }
                PrimitiveType.CHAR -> "reinterpretAsInt().reinterpretAsChar()"
                PrimitiveType.SHORT -> "reinterpretAsInt().reinterpretAsShort()"
                PrimitiveType.INT -> "reinterpretAsInt()"
                PrimitiveType.LONG -> "wasm_i64_extend_i32_s(this.toInt())"
                PrimitiveType.FLOAT -> "wasm_f32_convert_i32_s(this.toInt())"
                PrimitiveType.DOUBLE -> "wasm_f64_convert_i32_s(this.toInt())"
                else -> throw IllegalArgumentException("Unsupported type $returnTypeAsPrimitive for generation conversion method from type $thisKind")
            }
            PrimitiveType.INT -> when (returnTypeAsPrimitive) {
                PrimitiveType.BYTE -> "((this shl 24) shr 24).reinterpretAsByte()"
                PrimitiveType.CHAR -> "(this and 0xFFFF).reinterpretAsChar()"
                PrimitiveType.SHORT -> "((this shl 16) shr 16).reinterpretAsShort()"
                PrimitiveType.LONG -> "wasm_i64_extend_i32_s(this)"
                PrimitiveType.FLOAT -> "wasm_f32_convert_i32_s(this)"
                PrimitiveType.DOUBLE -> "wasm_f64_convert_i32_s(this)"
                else -> throw IllegalArgumentException("Unsupported type $returnTypeAsPrimitive for generation conversion method from type $thisKind")
            }
            PrimitiveType.LONG -> when (returnTypeAsPrimitive) {
                PrimitiveType.BYTE, PrimitiveType.CHAR, PrimitiveType.SHORT -> "this.toInt().to${returnTypeAsPrimitive.capitalized}()"
                    .also { this.signature.isInline = true }
                PrimitiveType.INT -> "wasm_i32_wrap_i64(this)"
                PrimitiveType.FLOAT -> "wasm_f32_convert_i64_s(this)"
                PrimitiveType.DOUBLE -> "wasm_f64_convert_i64_s(this)"
                else -> throw IllegalArgumentException("Unsupported type $returnTypeAsPrimitive for generation conversion method from type $thisKind")
            }
            in PrimitiveType.floatingPoint -> when (returnTypeAsPrimitive) {
                PrimitiveType.BYTE, PrimitiveType.CHAR, PrimitiveType.SHORT -> "this.toInt().to${returnTypeAsPrimitive.capitalized}()"
                    .also { this.signature.isInline = true }
                PrimitiveType.INT -> "wasm_i32_trunc_sat_${thisKind.prefixLowercase}_s(this)"
                PrimitiveType.LONG -> "wasm_i64_trunc_sat_${thisKind.prefixLowercase}_s(this)"
                PrimitiveType.FLOAT -> "wasm_f32_demote_f64(this)"
                PrimitiveType.DOUBLE -> "wasm_f64_promote_f32(this)"
                else -> throw IllegalArgumentException("Unsupported type $returnTypeAsPrimitive for generation conversion method from type $thisKind")
            }
            else -> throw IllegalArgumentException("Unsupported type $thisKind to generate conversion methods")
        }.addAsSingleLineBody(bodyOnNewLine = false)
    }

    override fun MethodDescription.modifyGeneratedEquals(thisKind: PrimitiveType) {
        val argName = this.signature.arg!!.name
        val additionalCheck = when (thisKind) {
            PrimitiveType.LONG -> "wasm_i64_eq(this, $argName)"
            PrimitiveType.FLOAT -> "this.equals(other)"
            PrimitiveType.DOUBLE -> "this.toBits() == other.toBits()"
            else -> {
                "wasm_i32_eq(this${thisKind.castToIfNecessary(PrimitiveType.INT)}, $argName${thisKind.castToIfNecessary(PrimitiveType.INT)})"
            }
        }
        "\t$argName is ${thisKind.capitalized} && $additionalCheck".addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodDescription.modifyGeneratedToString(thisKind: PrimitiveType) {
        when (thisKind) {
            in PrimitiveType.floatingPoint -> "dtoa(this${thisKind.castToIfNecessary(PrimitiveType.DOUBLE)})"
            PrimitiveType.INT, PrimitiveType.LONG -> "itoa${thisKind.bitSize}(this, 10)"
            else -> "this.toInt().toString()"
        }.addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun generateAdditionalMethods(thisKind: PrimitiveType): List<MethodDescription> {
        val hashCode = MethodDescription(
            doc = null,
            signature = MethodSignature(
                isOverride = true,
                isInline = true,
                name = "hashCode",
                arg = null,
                returnType = PrimitiveType.INT.capitalized
            )
        ).apply {
            when (thisKind) {
                PrimitiveType.LONG -> "return ((this ushr 32) xor this).toInt()".addAsMultiLineBody()
                PrimitiveType.FLOAT -> "toBits()".addAsSingleLineBody()
                PrimitiveType.DOUBLE -> "toBits().hashCode()".addAsSingleLineBody()
                else -> "return this${thisKind.castToIfNecessary(PrimitiveType.INT)}".addAsMultiLineBody()
            }
        }

        val customEquals = MethodDescription(
            doc = null,
            annotations = mutableListOf("kotlin.internal.IntrinsicConstEvaluation"),
            signature = MethodSignature(
                isInline = thisKind in PrimitiveType.floatingPoint,
                name = "equals",
                arg = MethodParameter("other", thisKind.capitalized),
                returnType = PrimitiveType.BOOLEAN.capitalized
            )
        ).apply {
            when (thisKind) {
                in PrimitiveType.floatingPoint -> "toBits() == other.toBits()".addAsSingleLineBody(bodyOnNewLine = false)
                else -> implementAsIntrinsic(thisKind, this.signature.name)
            }
        }

        val reinterprets = setOf(PrimitiveType.INT, PrimitiveType.BOOLEAN, PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.CHAR)
            .map {
                MethodDescription(
                    doc = null,
                    annotations = mutableListOf("WasmNoOpCast", "PublishedApi"),
                    signature = MethodSignature(
                        visibility = MethodVisibility.INTERNAL,
                        name = "reinterpretAs${it.capitalized}",
                        arg = null,
                        returnType = it.capitalized
                    )
                ).apply { "implementedAsIntrinsic".addAsSingleLineBody(bodyOnNewLine = true) }
            }

        val reinterpretInt = reinterprets.first()
        val otherReinterprets = reinterprets.drop(1).toTypedArray()

        return when (thisKind) {
            PrimitiveType.BYTE, PrimitiveType.SHORT -> listOf(customEquals, hashCode, reinterpretInt)
            PrimitiveType.INT -> listOf(customEquals, hashCode, *otherReinterprets)
            else -> listOf(customEquals, hashCode)
        }
    }

    companion object {
        private fun String.toWasmOperator(): String {
            return when (this) {
                "plus" -> "ADD"
                "minus" -> "SUB"
                "times" -> "MUL"
                "rem" -> "REM_S"
                "unaryMinus" -> "NEG"
                "shr" -> "SHR_S"
                "ushr" -> "SHR_U"
                "equals" -> "EQ"
                else -> this.uppercase()
            }
        }

        private fun MethodDescription.implementAsIntrinsic(thisKind: PrimitiveType, methodName: String) {
            this.signature.isInline = false
            addAnnotation("WasmOp(WasmOp.${thisKind.prefixUppercase}_${methodName.toWasmOperator()})")
            "implementedAsIntrinsic".addAsSingleLineBody(bodyOnNewLine = true)
        }

        private val PrimitiveType.prefixUppercase: String
            get() = when (this) {
                PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.INT -> "I32"
                PrimitiveType.LONG -> "I64"
                PrimitiveType.FLOAT -> "F32"
                PrimitiveType.DOUBLE -> "F64"
                else -> ""
            }

        private val PrimitiveType.prefixLowercase: String
            get() = prefixUppercase.lowercase()
    }
}
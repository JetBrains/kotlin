/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import java.io.PrintWriter
import java.util.*

class NativePrimitivesGenerator(writer: PrintWriter) : BasePrimitivesGenerator(writer) {
    override fun FileBuilder.modifyGeneratedFile() {
        suppress("OVERRIDE_BY_INLINE")
        suppress("NOTHING_TO_INLINE")
        import("kotlin.native.internal.*")
    }

    override fun CompanionObjectBuilder.modifyGeneratedCompanionObject(thisKind: PrimitiveType) {
        if (thisKind !in PrimitiveType.floatingPoint) {
            annotations += "CanBePrecreated"
        }
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

    override fun PropertyBuilder.modifyGeneratedCompanionObjectProperty(thisKind: PrimitiveType) {
        if (this.name in setOf("POSITIVE_INFINITY", "NEGATIVE_INFINITY", "NaN")) {
            annotations += "Suppress(\"DIVISION_BY_ZERO\")"
        }
    }

    override fun MethodBuilder.modifyGeneratedCompareTo(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        if (otherKind == thisKind) {
            if (thisKind in PrimitiveType.floatingPoint) {
                """
                    // if any of values in NaN both comparisons return false
                    if (this > $parameterName) return 1
                    if (this < $parameterName) return -1
            
                    val thisBits = this.toBits()
                    val otherBits = $parameterName.toBits()
            
                    // Canonical NaN bit representation is higher than any other value's bit representation
                    return thisBits.compareTo(otherBits)
                """.trimIndent().setAsBlockBody()
            } else {
                setAsExternal(thisKind)
            }
            return
        }

        modifySignature { isInline = thisKind !in PrimitiveType.floatingPoint }
        val thisCasted = "this" + thisKind.castToIfNecessary(otherKind)
        val otherCasted = parameterName + otherKind.castToIfNecessary(thisKind)
        if (thisKind == PrimitiveType.FLOAT && otherKind == PrimitiveType.DOUBLE) {
            "-${otherCasted}.compareTo(this)"
        } else {
            "$thisCasted.compareTo($otherCasted)"
        }.setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedBinaryOperation(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        val sign = operatorSign(this.methodName)

        if (thisKind != PrimitiveType.BYTE && thisKind != PrimitiveType.SHORT && thisKind == otherKind) {
            return setAsExternal(thisKind)
        }

        modifySignature { isInline = true }
        val returnTypeAsPrimitive = PrimitiveType.valueOf(returnType.uppercase())
        val thisCasted = "this" + thisKind.castToIfNecessary(returnTypeAsPrimitive)
        val otherCasted = parameterName + parameterType.toPrimitiveType().castToIfNecessary(returnTypeAsPrimitive)
        "$thisCasted $sign $otherCasted".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedUnaryOperation(thisKind: PrimitiveType) {
        if (methodName in setOf("inc", "dec") || thisKind == PrimitiveType.INT ||
            thisKind in PrimitiveType.floatingPoint || (methodName == "unaryMinus" && thisKind == PrimitiveType.LONG)
        ) {
            return setAsExternal(thisKind)
        }

        modifySignature { isInline = true }
        val returnTypeAsPrimitive = PrimitiveType.valueOf(returnType.uppercase())
        val thisCasted = "this" + thisKind.castToIfNecessary(returnTypeAsPrimitive)
        val sign = if (methodName == "unaryMinus") "-" else ""
        "$sign$thisCasted".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedBitShiftOperators(thisKind: PrimitiveType) {
        setAsExternal(thisKind)
    }

    override fun MethodBuilder.modifyGeneratedBitwiseOperators(thisKind: PrimitiveType) {
        setAsExternal(thisKind)
    }

    override fun MethodBuilder.modifyGeneratedConversions(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        when {
            otherKind == thisKind -> {
                modifySignature { isInline = true }
                "this".setAsExpressionBody()
            }
            thisKind !in PrimitiveType.floatingPoint -> {
                modifySignature { isExternal = true }
                val intrinsicType = when {
                    otherKind in PrimitiveType.floatingPoint -> "SIGNED_TO_FLOAT"
                    otherKind.byteSize < thisKind.byteSize -> "INT_TRUNCATE"
                    otherKind.byteSize > thisKind.byteSize -> "SIGN_EXTEND"
                    else -> "ZERO_EXTEND"
                }
                annotations += "TypedIntrinsic(IntrinsicType.$intrinsicType)"
            }
            else -> {
                if (otherKind in setOf(PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.CHAR)) {
                    "this.toInt().to${returnType}()".setAsExpressionBody()
                    return
                }

                modifySignature { isExternal = true }
                when {
                    otherKind in setOf(PrimitiveType.INT, PrimitiveType.LONG) -> {
                        annotations += "GCUnsafeCall(\"Kotlin_${thisKind.capitalized}_to${returnType}\")"
                    }
                    thisKind.byteSize > otherKind.byteSize -> {
                        annotations += "TypedIntrinsic(IntrinsicType.FLOAT_TRUNCATE)"
                    }
                    thisKind.byteSize < otherKind.byteSize -> {
                        annotations += "TypedIntrinsic(IntrinsicType.FLOAT_EXTEND)"
                    }
                }
            }
        }
    }

    override fun MethodBuilder.modifyGeneratedEquals(thisKind: PrimitiveType) {
        val additionalCheck = if (thisKind in PrimitiveType.floatingPoint) {
            "toBits() == other.toBits()"
        } else {
            "kotlin.native.internal.areEqualByValue(this, $parameterName)"
        }
        "$parameterName is ${thisKind.capitalized} && $additionalCheck".setAsExpressionBody()
    }

    override fun MethodBuilder.modifyGeneratedToString(thisKind: PrimitiveType) {
        if (thisKind in PrimitiveType.floatingPoint) {
            appendDoc("Returns the string representation of this [${thisKind.capitalized}] value.\n")
            """
            Note that the representation format is unstable and may change in a future release.
            However, it is guaranteed that the returned string is valid for converting back to [${thisKind.capitalized}]
            using [String.to${thisKind.capitalized}], and will result in the same numeric value.
            The exact bit pattern of a `NaN` ${thisKind.name.lowercase()} is not guaranteed to be preserved though.
            """.trimIndent().let { appendDoc(it) }

            "NumberConverter.convert(this)".setAsExpressionBody()
        } else {
            modifySignature { isExternal = true }
            annotations += "GCUnsafeCall(\"Kotlin_${thisKind.capitalized}_toString\")"
        }
    }

    override fun ClassBuilder.generateAdditionalMethods(thisKind: PrimitiveType) {
        generateCustomEquals(thisKind)
        generateHashCode(thisKind)
        if (thisKind in PrimitiveType.floatingPoint) {
            generateBits(thisKind)
        }
    }

    private fun ClassBuilder.generateHashCode(thisKind: PrimitiveType) {
        method {
            signature {
                isOverride = true
                methodName = "hashCode"
                returnType = PrimitiveType.INT.capitalized
            }

            when (thisKind) {
                PrimitiveType.LONG -> "((this ushr 32) xor this).toInt()"
                PrimitiveType.FLOAT -> "toBits()"
                PrimitiveType.DOUBLE -> "toBits().hashCode()"
                else -> "this${thisKind.castToIfNecessary(PrimitiveType.INT)}"
            }.setAsExpressionBody()
        }
    }

    private fun ClassBuilder.generateCustomEquals(thisKind: PrimitiveType) {
        method {
            annotations += "Deprecated(\"Provided for binary compatibility\", level = DeprecationLevel.HIDDEN)"
            annotations += intrinsicConstEvaluationAnnotation
            signature {
                methodName = "equals"
                parameter {
                    name = "other"
                    type = thisKind.capitalized
                }
                returnType = PrimitiveType.BOOLEAN.capitalized
            }

            when (thisKind) {
                in PrimitiveType.floatingPoint -> "toBits() == other.toBits()".setAsExpressionBody()
                else -> "kotlin.native.internal.areEqualByValue(this, other)".setAsExpressionBody()
            }
        }
    }

    private fun ClassBuilder.generateBits(thisKind: PrimitiveType) {
        method {
            signature {
                isExternal = true
                visibility = MethodVisibility.INTERNAL
                methodName = "bits"
                returnType = if (thisKind == PrimitiveType.FLOAT) PrimitiveType.INT.capitalized else PrimitiveType.LONG.capitalized
            }

            annotations += "TypedIntrinsic(IntrinsicType.REINTERPRET)"
            annotations += "PublishedApi"
        }
    }

    companion object {
        private fun String.toNativeOperator(thisKind: PrimitiveType): String {
            return when {
                this == "div" || this == "rem" -> {
                    val prefix = if (thisKind == PrimitiveType.BOOLEAN || thisKind == PrimitiveType.CHAR) "UNSIGNED" else "SIGNED"
                    "${prefix}_${this.uppercase()}"
                }
                this == "compareTo" -> {
                    val prefix = if (thisKind == PrimitiveType.BOOLEAN || thisKind == PrimitiveType.CHAR) "UNSIGNED" else "SIGNED"
                    "${prefix}_COMPARE_TO"
                }
                this.startsWith("unary") -> "UNARY_${this.replace("unary", "").uppercase()}"
                else -> this.uppercase()
            }
        }

        internal fun MethodBuilder.setAsExternal(thisKind: PrimitiveType) {
            annotations += "TypedIntrinsic(IntrinsicType.${methodName.toNativeOperator(thisKind)})"
            modifySignature { isExternal = true }
        }
    }
}
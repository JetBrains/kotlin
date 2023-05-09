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

    override fun ClassBuilder.modifyGeneratedClass(thisKind: PrimitiveType) {
        this.isFinal = true
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
                """.trimIndent().addAsMultiLineBody()
            } else {
                setAsExternal()
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
        }.addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodBuilder.modifyGeneratedBinaryOperation(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        val sign = operatorSign(this.methodName)

        if (thisKind != PrimitiveType.BYTE && thisKind != PrimitiveType.SHORT && thisKind == otherKind) {
            return setAsExternal()
        }

        modifySignature { isInline = true }
        val returnTypeAsPrimitive = PrimitiveType.valueOf(returnType.uppercase())
        val thisCasted = "this" + thisKind.castToIfNecessary(returnTypeAsPrimitive)
        val otherCasted = parameterName + parameterType.toPrimitiveType().castToIfNecessary(returnTypeAsPrimitive)
        "$thisCasted $sign $otherCasted".addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodBuilder.modifyGeneratedUnaryOperation(thisKind: PrimitiveType) {
        if (methodName in setOf("inc", "dec") || thisKind == PrimitiveType.INT ||
            thisKind in PrimitiveType.floatingPoint || (methodName == "unaryMinus" && thisKind == PrimitiveType.LONG)
        ) {
            return setAsExternal()
        }

        modifySignature { isInline = true }
        val returnTypeAsPrimitive = PrimitiveType.valueOf(returnType.uppercase())
        val thisCasted = "this" + thisKind.castToIfNecessary(returnTypeAsPrimitive)
        val sign = if (methodName == "unaryMinus") "-" else ""
        "$sign$thisCasted".addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodBuilder.modifyGeneratedRangeTo(thisKind: PrimitiveType) {
        val rangeType = PrimitiveType.valueOf(returnType.replace("Range", "").uppercase())
        val thisCasted = "this" + thisKind.castToIfNecessary(rangeType)
        val otherCasted = parameterName + parameterType.toPrimitiveType().castToIfNecessary(rangeType)
        "return ${returnType}($thisCasted, $otherCasted)".addAsMultiLineBody()
    }

    override fun MethodBuilder.modifyGeneratedRangeUntil(thisKind: PrimitiveType) {
        "this until $parameterName".addAsSingleLineBody(bodyOnNewLine = false)
    }

    override fun MethodBuilder.modifyGeneratedBitShiftOperators(thisKind: PrimitiveType) {
        setAsExternal()
    }

    override fun MethodBuilder.modifyGeneratedBitwiseOperators(thisKind: PrimitiveType) {
        setAsExternal()
    }

    override fun MethodBuilder.modifyGeneratedConversions(thisKind: PrimitiveType) {
        val returnTypeAsPrimitive = PrimitiveType.valueOf(returnType.uppercase())
        when {
            returnTypeAsPrimitive == thisKind -> {
                modifySignature { isInline = true }
                "this".addAsSingleLineBody(bodyOnNewLine = true)
            }
            thisKind !in PrimitiveType.floatingPoint -> {
                modifySignature { isExternal = true }
                val intrinsicType = when {
                    returnTypeAsPrimitive in PrimitiveType.floatingPoint -> "SIGNED_TO_FLOAT"
                    returnTypeAsPrimitive.byteSize < thisKind.byteSize -> "INT_TRUNCATE"
                    returnTypeAsPrimitive.byteSize > thisKind.byteSize -> "SIGN_EXTEND"
                    else -> "ZERO_EXTEND"
                }
                annotations += "TypedIntrinsic(IntrinsicType.$intrinsicType)"
            }
            else -> {
                if (returnTypeAsPrimitive in setOf(PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.CHAR)) {
                    "this.toInt().to${returnType}()".addAsSingleLineBody(bodyOnNewLine = false)
                    return
                }

                modifySignature { isExternal = true }
                when {
                    returnTypeAsPrimitive in setOf(PrimitiveType.INT, PrimitiveType.LONG) -> {
                        annotations += "GCUnsafeCall(\"Kotlin_${thisKind.capitalized}_to${returnType}\")"
                    }
                    thisKind.byteSize > returnTypeAsPrimitive.byteSize -> {
                        annotations += "TypedIntrinsic(IntrinsicType.FLOAT_TRUNCATE)"
                    }
                    thisKind.byteSize < returnTypeAsPrimitive.byteSize -> {
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
        "$parameterName is ${thisKind.capitalized} && $additionalCheck".addAsSingleLineBody(bodyOnNewLine = true)
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

            "NumberConverter.convert(this)".addAsSingleLineBody(bodyOnNewLine = false)
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
            }.addAsSingleLineBody()
        }
    }

    private fun ClassBuilder.generateCustomEquals(thisKind: PrimitiveType) {
        method {
            annotations += "Deprecated(\"Provided for binary compatibility\", level = DeprecationLevel.HIDDEN)"
            annotations += "kotlin.internal.IntrinsicConstEvaluation"
            signature {
                methodName = "equals"
                parameter {
                    name = "other"
                    type = thisKind.capitalized
                }
                returnType = PrimitiveType.BOOLEAN.capitalized
            }

            when (thisKind) {
                in PrimitiveType.floatingPoint -> "toBits() == other.toBits()".addAsSingleLineBody(bodyOnNewLine = false)
                else -> "kotlin.native.internal.areEqualByValue(this, other)".addAsSingleLineBody(bodyOnNewLine = false)
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
        private fun String.toNativeOperator(): String {
            if (this == "div" || this == "rem") return "SIGNED_${this.uppercase(Locale.getDefault())}"
            if (this == "compareTo") return "SIGNED_COMPARE_TO"
            if (this.startsWith("unary")) return "UNARY_${this.replace("unary", "").uppercase(Locale.getDefault())}"
            return this.uppercase(Locale.getDefault())
        }

        private fun MethodBuilder.setAsExternal() {
            annotations += "TypedIntrinsic(IntrinsicType.${methodName.toNativeOperator()})"
            modifySignature { isExternal = true }
        }
    }
}
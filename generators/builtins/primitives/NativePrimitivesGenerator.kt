/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import java.io.PrintWriter
import java.util.*

class NativePrimitivesGenerator(writer: PrintWriter) : BasePrimitivesGenerator(writer) {
    override fun FileDescription.modifyGeneratedFile() {
        this.addSuppress("OVERRIDE_BY_INLINE")
        this.addSuppress("NOTHING_TO_INLINE")
        this.addImport("kotlin.native.internal.*")
    }

    override fun ClassDescription.modifyGeneratedClass(thisKind: PrimitiveType) {
        this.isFinal = true
    }

    override fun CompanionObjectDescription.modifyGeneratedCompanionObject(thisKind: PrimitiveType) {
        if (thisKind !in PrimitiveType.floatingPoint) {
            this.addAnnotation("CanBePrecreated")
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

    override fun PropertyDescription.modifyGeneratedCompanionObjectProperty(thisKind: PrimitiveType) {
        if (this.name in setOf("POSITIVE_INFINITY", "NEGATIVE_INFINITY", "NaN")) {
            this.addAnnotation("Suppress(\"DIVISION_BY_ZERO\")")
        }
    }

    override fun MethodDescription.modifyGeneratedCompareTo(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        if (otherKind == thisKind) {
            if (thisKind in PrimitiveType.floatingPoint) {
                val argName = this.signature.arg!!.name
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
                setAsExternal()
            }
            return
        }

        this.signature.isInline = thisKind !in PrimitiveType.floatingPoint
        val thisCasted = "this" + thisKind.castToIfNecessary(otherKind)
        val otherCasted = this.signature.arg!!.name + otherKind.castToIfNecessary(thisKind)
        if (thisKind == PrimitiveType.FLOAT && otherKind == PrimitiveType.DOUBLE) {
            "- ${otherCasted}.compareTo(this)"
        } else {
            "$thisCasted.compareTo($otherCasted)"
        }.addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodDescription.modifyGeneratedBinaryOperation(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        val sign = this.signature.name.asSign()

        if (thisKind != PrimitiveType.BYTE && thisKind != PrimitiveType.SHORT && thisKind == otherKind) {
            return setAsExternal()
        }

        this.signature.isInline = true
        val returnTypeAsPrimitive = PrimitiveType.valueOf(this.signature.returnType.uppercase())
        val thisCasted = "this" + thisKind.castToIfNecessary(returnTypeAsPrimitive)
        val otherCasted = this.signature.arg!!.name + this.signature.arg.getTypeAsPrimitive().castToIfNecessary(returnTypeAsPrimitive)
        "$thisCasted $sign $otherCasted".addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodDescription.modifyGeneratedUnaryOperation(thisKind: PrimitiveType) {
        if (this.signature.name in setOf("inc", "dec") || thisKind == PrimitiveType.INT ||
            thisKind in PrimitiveType.floatingPoint || (this.signature.name == "unaryMinus" && thisKind == PrimitiveType.LONG)
        ) {
            return setAsExternal()
        }

        this.signature.isInline = true
        val returnTypeAsPrimitive = PrimitiveType.valueOf(this.signature.returnType.uppercase())
        val thisCasted = "this" + thisKind.castToIfNecessary(returnTypeAsPrimitive)
        val sign = if (this.signature.name == "unaryMinus") "-" else ""
        "$sign$thisCasted".addAsSingleLineBody(bodyOnNewLine = true)
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
        setAsExternal()
    }

    override fun MethodDescription.modifyGeneratedBitwiseOperators(thisKind: PrimitiveType) {
        setAsExternal()
    }

    override fun MethodDescription.modifyGeneratedConversions(thisKind: PrimitiveType) {
        val returnTypeAsPrimitive = PrimitiveType.valueOf(this.signature.returnType.uppercase())
        when {
            returnTypeAsPrimitive == thisKind -> {
                this.signature.isInline = true
                "this".addAsSingleLineBody(bodyOnNewLine = true)
            }
            thisKind !in PrimitiveType.floatingPoint -> {
                this.signature.isExternal = true
                val intrinsicType = when {
                    returnTypeAsPrimitive in PrimitiveType.floatingPoint -> "SIGNED_TO_FLOAT"
                    returnTypeAsPrimitive.byteSize < thisKind.byteSize -> "INT_TRUNCATE"
                    returnTypeAsPrimitive.byteSize > thisKind.byteSize -> "SIGN_EXTEND"
                    else -> "ZERO_EXTEND"
                }
                this.addAnnotation("TypedIntrinsic(IntrinsicType.$intrinsicType)")
            }
            else -> {
                if (returnTypeAsPrimitive in setOf(PrimitiveType.BYTE, PrimitiveType.SHORT, PrimitiveType.CHAR)) {
                    "this.toInt().to${this.signature.returnType}()".addAsSingleLineBody(bodyOnNewLine = false)
                    return
                }

                this.signature.isExternal = true
                when {
                    returnTypeAsPrimitive in setOf(PrimitiveType.INT, PrimitiveType.LONG) -> {
                        this.addAnnotation("GCUnsafeCall(\"Kotlin_${thisKind.capitalized}_to${this.signature.returnType}\")")
                    }
                    thisKind.byteSize > returnTypeAsPrimitive.byteSize -> {
                        this.addAnnotation("TypedIntrinsic(IntrinsicType.FLOAT_TRUNCATE)")
                    }
                    thisKind.byteSize < returnTypeAsPrimitive.byteSize -> {
                        this.addAnnotation("TypedIntrinsic(IntrinsicType.FLOAT_EXTEND)")
                    }
                }
            }
        }
    }

    override fun MethodDescription.modifyGeneratedEquals(thisKind: PrimitiveType) {
        val argName = this.signature.arg!!.name
        val additionalCheck = if (thisKind in PrimitiveType.floatingPoint) {
            "this.equals(other)"
        } else {
            "kotlin.native.internal.areEqualByValue(this, $argName)"
        }
        "    $argName is ${thisKind.capitalized} && $additionalCheck".addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodDescription.modifyGeneratedToString(thisKind: PrimitiveType) {
        if (thisKind in PrimitiveType.floatingPoint) {
            "NumberConverter.convert(this)".addAsSingleLineBody(bodyOnNewLine = false)
        } else {
            this.signature.isExternal = true
            this.addAnnotation("GCUnsafeCall(\"Kotlin_${thisKind.capitalized}_toString\")")
        }
    }

    override fun generateAdditionalMethods(thisKind: PrimitiveType): List<MethodDescription> {
        val hashCode = MethodDescription(
            doc = null,
            signature = MethodSignature(
                isOverride = true,
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
                name = "equals",
                arg = MethodParameter("other", thisKind.capitalized),
                returnType = PrimitiveType.BOOLEAN.capitalized
            )
        ).apply {
            when (thisKind) {
                in PrimitiveType.floatingPoint -> "toBits() == other.toBits()".addAsSingleLineBody(bodyOnNewLine = false)
                else -> "kotlin.native.internal.areEqualByValue(this, other)".addAsSingleLineBody(bodyOnNewLine = false)
            }
        }

        val bits = MethodDescription(
            doc = null,
            signature = MethodSignature(
                isExternal = true,
                visibility = MethodVisibility.INTERNAL,
                name = "bits",
                arg = null,
                returnType = if (thisKind == PrimitiveType.FLOAT) PrimitiveType.INT.capitalized else PrimitiveType.LONG.capitalized
            )
        ).apply {
            this.addAnnotation("TypedIntrinsic(IntrinsicType.REINTERPRET)")
            this.addAnnotation("PublishedApi")
        }

        return if (thisKind in PrimitiveType.floatingPoint) {
            listOf(customEquals, hashCode, bits)
        } else {
            listOf(customEquals, hashCode)
        }
    }

    companion object {
        private fun String.toNativeOperator(): String {
            if (this == "div" || this == "rem") return "SIGNED_${this.uppercase(Locale.getDefault())}"
            if (this == "compareTo") return "SIGNED_COMPARE_TO"
            if (this.startsWith("unary")) return "UNARY_${this.replace("unary", "").uppercase(Locale.getDefault())}"
            return this.uppercase(Locale.getDefault())
        }

        private fun MethodDescription.setAsExternal() {
            addAnnotation("TypedIntrinsic(IntrinsicType.${this.signature.name.toNativeOperator()})")
            this.signature.isExternal = true
        }
    }
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import java.io.PrintWriter

class JsPrimitivesGenerator(writer: PrintWriter) : BasePrimitivesGenerator(writer) {
    private val intrinsifiedOverrideComment = """
        NOTE: This method is treated as an intrinsic when called directly: the compiler will emit different code for calls to it
        depending on whether Longs are represented as JS objects or as BigInt.
        However, since it's an override, it still has to have a body, so that virtual dispatch works correctly with type-erased
        boxed Longs.
        
        TODO(KT-70480): Make bodiless when we drop the ES5 target.
        """.trimIndent()

    private val makeBodilessAfterBootstrapAdvanceComment = "TODO: Make bodiless after bootstrap advance"

    override fun FileBuilder.modifyGeneratedFile() {
        suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")
        suppress("UNUSED_PARAMETER")
        import("kotlin.js.internal.boxedLong.*")
    }

    override fun PropertyBuilder.modifyGeneratedCompanionObjectProperty(thisKind: PrimitiveType) {
        if (this.name in setOf("POSITIVE_INFINITY", "NEGATIVE_INFINITY", "NaN")) {
            annotations += "Suppress(\"DIVISION_BY_ZERO\")"
        }
    }

    override fun ClassBuilder.modifyGeneratedClass(thisKind: PrimitiveType) {
        // TODO(KT-70480): Remove this when we drop ES5 support
        if (thisKind == PrimitiveType.LONG) {
            annotations += "Suppress(\"NOTHING_TO_INLINE\")"
            primaryConstructor {
                annotations += "OptIn(BoxedLongImplementation::class)"
                visibility = MethodVisibility.INTERNAL
                parameter {
                    name = "@OptIn(BoxedLongImplementation::class) internal val low"
                    type = PrimitiveType.INT.capitalized
                }
                parameter {
                    name = "@OptIn(BoxedLongImplementation::class) internal val high"
                    type = PrimitiveType.INT.capitalized
                }
            }
        }
    }

    override fun MethodBuilder.modifyGeneratedCompareTo(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        modifyGeneratedBinaryOperation(thisKind, otherKind)
    }

    override fun MethodBuilder.modifyGeneratedBinaryOperation(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        if (thisKind == PrimitiveType.LONG) {
            if (thisKind == otherKind) {
                additionalComments = if (methodName == "compareTo") {
                    intrinsifiedOverrideComment
                } else {
                    makeBodilessAfterBootstrapAdvanceComment
                }
                val implMethod = when (methodName) {
                    "compareTo" -> "compare"
                    "plus" -> "add"
                    "minus" -> "subtract"
                    "times" -> "multiply"
                    "div" -> "divide"
                    "rem" -> "modulo"
                    else -> error("Unsupported binary operation: $methodName")
                }
                "this.$implMethod(other)".setAsExpressionBody()
                annotations += "OptIn(BoxedLongImplementation::class)"
            } else {
                modifySignature { isInline = true }
                "this${thisKind.castToIfNecessary(otherKind)}.${methodName}(other${otherKind.castToIfNecessary(thisKind)})".setAsExpressionBody()
            }
        }
    }

    override fun MethodBuilder.modifyGeneratedUnaryOperation(thisKind: PrimitiveType) {
        if (thisKind == PrimitiveType.LONG) {
            when (methodName) {
                "inc" -> "this + 1L"
                "dec" -> "this - 1L"
                "unaryMinus" -> {
                    additionalComments = makeBodilessAfterBootstrapAdvanceComment
                    "this.inv() + 1L"
                }
                "unaryPlus" -> {
                    modifySignature { isInline = true }
                    "this"
                }
                else -> error(methodName)
            }.setAsExpressionBody()
        }
    }

    override fun MethodBuilder.modifyGeneratedRangeTo(thisKind: PrimitiveType, otherKind: PrimitiveType, opReturnType: PrimitiveType) {
        if (thisKind != PrimitiveType.LONG) {
            noBody()
        }
    }

    override fun MethodBuilder.modifyGeneratedBitShiftOperators(thisKind: PrimitiveType) {
        // TODO: Remove this override after bootstrap advance
        if (thisKind == PrimitiveType.LONG) {
            additionalComments = makeBodilessAfterBootstrapAdvanceComment
            val implMethod = when (methodName) {
                "shl" -> "shiftLeft"
                "shr" -> "shiftRight"
                "ushr" -> "shiftRightUnsigned"
                else -> error(methodName)
            }
            "$implMethod(bitCount)".setAsExpressionBody()
            annotations += "OptIn(BoxedLongImplementation::class)"
        }
    }

    override fun MethodBuilder.modifyGeneratedBitwiseOperators(thisKind: PrimitiveType) {
        if (thisKind == PrimitiveType.LONG) {
            additionalComments = makeBodilessAfterBootstrapAdvanceComment
            if (methodName == "inv") {
                "invert()".setAsExpressionBody()
            } else {
                "bitwise${methodName.replaceFirstChar(Char::uppercaseChar)}(other)".setAsExpressionBody()
            }
            annotations += "OptIn(BoxedLongImplementation::class)"
        }
    }

    override fun MethodBuilder.modifyGeneratedConversions(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        if (thisKind == PrimitiveType.LONG) {
            additionalComments = intrinsifiedOverrideComment
            var assumesBoxedImplementation = false
            when (otherKind) {
                PrimitiveType.CHAR,
                PrimitiveType.BYTE,
                PrimitiveType.SHORT,
                PrimitiveType.INT -> {
                    assumesBoxedImplementation = true
                    "convertTo${otherKind.capitalized}()"
                }
                PrimitiveType.LONG -> "this"
                PrimitiveType.FLOAT -> "toDouble().toFloat()"
                PrimitiveType.DOUBLE -> {
                    assumesBoxedImplementation = true
                    "toNumber()"
                }
                else -> error("Unsupported type $otherKind for Long conversion")
            }.setAsExpressionBody()
            if (assumesBoxedImplementation) {
                annotations += "OptIn(BoxedLongImplementation::class)"
            }
        }
    }

    override fun MethodBuilder.modifyGeneratedEquals(thisKind: PrimitiveType) {
        if (thisKind == PrimitiveType.LONG) {
            additionalComments = intrinsifiedOverrideComment
            "other is Long && equalsLong(other)".setAsExpressionBody()
            annotations += "OptIn(BoxedLongImplementation::class)"
        }
    }

    override fun MethodBuilder.modifyGeneratedToString(thisKind: PrimitiveType) {
        if (thisKind == PrimitiveType.LONG) {
            additionalComments = "TODO(KT-70480): Make bodiless when we drop the ES5 target."
            "this.toStringImpl(radix = 10)".setAsExpressionBody()
            annotations += "OptIn(BoxedLongImplementation::class)"
        }
    }

    override fun MethodBuilder.modifyGeneratedHashCode(thisKind: PrimitiveType) {
        if (thisKind == PrimitiveType.LONG) {
            additionalComments = intrinsifiedOverrideComment
            "hashCode(this)".setAsExpressionBody()
            annotations += "OptIn(BoxedLongImplementation::class)"
        } else {
            noBody()
        }
    }

    override fun ClassBuilder.generateAdditionalMethods(thisKind: PrimitiveType) {
        generateHashCode(thisKind)

        if (thisKind == PrimitiveType.LONG) {
            method {
                additionalComments = """
                    This method is used by JavaScript to convert objects of type Long to primitives.
                    This is essential for the JavaScript interop.
                    JavaScript functions that expect `number` are imported in Kotlin as expecting `kotlin.Number`
                    (in our standard library, and also in user projects if they use Dukat for generating external declarations).
                    Because `kotlin.Number` is a supertype of `Long` too, there has to be a way for JS to know how to handle Longs.
                    See KT-50202
                    
                    TODO(KT-70480): Remove this when we drop ES5 support
                """.trimIndent()
                annotations += "JsName(\"valueOf\")"
                expectActual = ExpectActualModifier.Unspecified
                signature {
                    visibility = MethodVisibility.INTERNAL
                    methodName = "valueOf"
                    returnType = PrimitiveType.DOUBLE.capitalized
                }
                "toDouble()".setAsExpressionBody()
            }
        }
    }
}

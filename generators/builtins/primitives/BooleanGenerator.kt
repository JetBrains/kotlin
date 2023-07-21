/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.*
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.NativePrimitivesGenerator.Companion.setAsExternal
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.WasmPrimitivesGenerator.Companion.implementAsIntrinsic
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.WasmPrimitivesGenerator.Companion.implementedAsIntrinsic
import java.io.PrintWriter

abstract class BooleanGenerator(private val writer: PrintWriter) : BuiltInsGenerator {
    override fun generate() {
        writer.print(generateFile().build())
    }

    private fun generateFile(): FileBuilder {
        return file { generateClass() }.apply { this.modifyGeneratedFile() }
    }

    private fun FileBuilder.generateClass() {
        klass {
            appendDoc("Represents a value which is either `true` or `false`.")
            name = PrimitiveType.BOOLEAN.capitalized
            superType("Comparable<$name>")

            generateCompanionObject()

            generateNot()
            generateAnd()
            generateOr()
            generateXor()
            generateCompareTo()

            generateToString()
            generateEquals()
            generateHashCode()
            generateAdditionalMethods()
        }.modifyGeneratedClass()
    }

    private fun ClassBuilder.generateCompanionObject() {
        companionObject {
            annotations += "SinceKotlin(\"1.3\")"
        }.modifyGeneratedCompanionObject()
    }

    private fun ClassBuilder.generateNot() {
        method {
            appendDoc("Returns the inverse of this boolean.")
            annotations += intrinsicConstEvaluationAnnotation
            signature {
                methodName = "not"
                isOperator = true
                returnType = PrimitiveType.BOOLEAN.capitalized
            }

        }.modifyGeneratedNot()
    }

    private fun ClassBuilder.generateBooleanMethod(giveMethodName: String, doc: String) = method {
        appendDoc(doc)
        annotations += intrinsicConstEvaluationAnnotation
        signature {
            methodName = giveMethodName
            parameter {
                name = "other"
                type = PrimitiveType.BOOLEAN.capitalized
            }
            isInfix = true
            returnType = PrimitiveType.BOOLEAN.capitalized
        }
    }

    private fun ClassBuilder.generateAnd() {
        val doc = """
            Performs a logical `and` operation between this Boolean and the [other] one. Unlike the `&&` operator,
            this function does not perform short-circuit evaluation. Both `this` and [other] will always be evaluated.
        """.trimIndent()
        generateBooleanMethod("and", doc).modifyGeneratedAnd()
    }

    private fun ClassBuilder.generateOr() {
        val doc = """
            Performs a logical `or` operation between this Boolean and the [other] one. Unlike the `||` operator,
            this function does not perform short-circuit evaluation. Both `this` and [other] will always be evaluated.
        """.trimIndent()
        generateBooleanMethod("or", doc).modifyGeneratedOr()
    }

    private fun ClassBuilder.generateXor() {
        val doc = "Performs a logical `xor` operation between this Boolean and the [other] one."
        generateBooleanMethod("xor", doc).modifyGeneratedXor()
    }

    private fun ClassBuilder.generateCompareTo() {
        method {
            annotations += intrinsicConstEvaluationAnnotation
            signature {
                methodName = "compareTo"
                parameter {
                    name = "other"
                    type = PrimitiveType.BOOLEAN.capitalized
                }
                isOverride = true
                returnType = PrimitiveType.INT.capitalized
            }
        }.modifyGeneratedCompareTo()
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
    internal open fun MethodBuilder.modifyGeneratedNot() {}
    internal open fun MethodBuilder.modifyGeneratedAnd() {}
    internal open fun MethodBuilder.modifyGeneratedOr() {}
    internal open fun MethodBuilder.modifyGeneratedXor() {}
    internal open fun MethodBuilder.modifyGeneratedCompareTo() {}
    internal open fun MethodBuilder.modifyGeneratedToString() {}
    internal open fun MethodBuilder.modifyGeneratedEquals() {}
    internal open fun MethodBuilder.modifyGeneratedHashCode() {}
    internal open fun ClassBuilder.generateAdditionalMethods() {}
}

class JvmBooleanGenerator(writer: PrintWriter) : BooleanGenerator(writer) {
    override fun ClassBuilder.modifyGeneratedClass() {
        appendDoc("On the JVM, non-nullable values of this type are represented as values of the primitive type `boolean`.")
    }
}

class JsBooleanGenerator(writer: PrintWriter) : BooleanGenerator(writer) {
    override fun FileBuilder.modifyGeneratedFile() {
        suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")
        suppress("UNUSED_PARAMETER")
    }
}

class WasmBooleanGenerator(writer: PrintWriter) : BooleanGenerator(writer) {
    override fun FileBuilder.modifyGeneratedFile() {
        suppress("UNUSED_PARAMETER")
        import("kotlin.wasm.internal.*")
    }

    override fun ClassBuilder.modifyGeneratedClass() {
        annotations += "WasmAutoboxed"
        primaryConstructor {
            parameter {
                name = "private val value"
                type = PrimitiveType.BOOLEAN.capitalized
            }
        }
    }

    override fun CompanionObjectBuilder.modifyGeneratedCompanionObject() {
        isPublic = true
    }

    override fun MethodBuilder.modifyGeneratedNot() {
        implementAsIntrinsic(PrimitiveType.BOOLEAN, methodName)
    }

    override fun MethodBuilder.modifyGeneratedAnd() {
        implementAsIntrinsic(PrimitiveType.BOOLEAN, methodName)
    }

    override fun MethodBuilder.modifyGeneratedOr() {
        implementAsIntrinsic(PrimitiveType.BOOLEAN, methodName)
    }

    override fun MethodBuilder.modifyGeneratedXor() {
        implementAsIntrinsic(PrimitiveType.BOOLEAN, methodName)
    }

    override fun MethodBuilder.modifyGeneratedCompareTo() {
        "wasm_i32_compareTo(this.toInt(), other.toInt())".addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodBuilder.modifyGeneratedToString() {
        modifySignature { visibility = null }
        "if (this) \"true\" else \"false\"".addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun MethodBuilder.modifyGeneratedEquals() {
        modifySignature { visibility = null }
        """
            return if (other !is Boolean) {
                false
            } else {
                wasm_i32_eq(this.toInt(), other.toInt())
            }
        """.trimIndent().addAsMultiLineBody()
    }

    override fun MethodBuilder.modifyGeneratedHashCode() {
        modifySignature { visibility = null }
        "if (this) 1231 else 1237".addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun ClassBuilder.generateAdditionalMethods() {
        method {
            annotations += "WasmNoOpCast"
            signature {
                visibility = MethodVisibility.INTERNAL
                methodName = "toInt"
                returnType = PrimitiveType.INT.capitalized
            }
            implementedAsIntrinsic.addAsSingleLineBody(bodyOnNewLine = true)
        }
    }
}

class NativeBooleanGenerator(writer: PrintWriter) : BooleanGenerator(writer) {
    override fun FileBuilder.modifyGeneratedFile() {
        import("kotlin.native.internal.TypedIntrinsic")
        import("kotlin.native.internal.IntrinsicType")
    }

    override fun MethodBuilder.modifyGeneratedNot() {
        setAsExternal(PrimitiveType.BOOLEAN)
    }

    override fun MethodBuilder.modifyGeneratedAnd() {
        setAsExternal(PrimitiveType.BOOLEAN)
    }

    override fun MethodBuilder.modifyGeneratedOr() {
        setAsExternal(PrimitiveType.BOOLEAN)
    }

    override fun MethodBuilder.modifyGeneratedXor() {
        setAsExternal(PrimitiveType.BOOLEAN)
    }

    override fun MethodBuilder.modifyGeneratedCompareTo() {
        setAsExternal(PrimitiveType.BOOLEAN)
    }

    override fun MethodBuilder.modifyGeneratedToString() {
        "if (this) \"true\" else \"false\"".addAsSingleLineBody()
    }

    override fun MethodBuilder.modifyGeneratedEquals() {
        "other is Boolean && kotlin.native.internal.areEqualByValue(this, other)".addAsSingleLineBody(bodyOnNewLine = true)
    }

    private fun ClassBuilder.generateCustomEquals() {
        method {
            annotations += "Deprecated(\"Provided for binary compatibility\", level = DeprecationLevel.HIDDEN)"
            annotations += intrinsicConstEvaluationAnnotation
            signature {
                methodName = "equals"
                parameter {
                    name = "other"
                    type = PrimitiveType.BOOLEAN.capitalized
                }
                returnType = PrimitiveType.BOOLEAN.capitalized
            }

            "kotlin.native.internal.areEqualByValue(this, other)".addAsSingleLineBody(bodyOnNewLine = false)
        }
    }

    override fun MethodBuilder.modifyGeneratedHashCode() {
        modifySignature { visibility = MethodVisibility.PUBLIC }
        "if (this) 1231 else 1237".addAsSingleLineBody(bodyOnNewLine = true)
    }

    override fun ClassBuilder.generateAdditionalMethods() {
        generateCustomEquals()
    }
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.arrays

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.*
import java.io.PrintWriter

abstract class GenerateArrays(val writer: PrintWriter, val primitiveArrays: Boolean) : BuiltInsGenerator {
    override fun generate() {
        writer.print(generateFile().build())
    }

    private fun generateFile(): FileBuilder {
        return file(this::class) { generateClasses() }.apply { this.modifyGeneratedFile() }
    }

    internal abstract class ArrayBuilder(
        val kind: PrimitiveType?,
        val generateRuntimeTypeAppendix: AnnotatedAndDocumented.(String) -> Unit = {},
    ) {
        protected val arrayClassName = "${kind?.capitalized ?: ""}Array"
        protected val arrayTypeName = arrayClassName + if (kind == null) "<T>" else ""
        protected val elementTypeName = kind?.capitalized ?: "T"
        protected val iteratorClassName = "${kind?.capitalized ?: ""}Iterator"
        protected val arrayIteratorImplClassName = "${kind?.capitalized ?: ""}ArrayIterator"

        fun FileBuilder.generateArrayClass() {
            val typeLower = kind?.name?.lowercase() ?: "T"
            klass {
                expectActual = ExpectActualModifier.Actual
                name = arrayClassName
                if (kind == null) {
                    appendDoc("A generic array of objects.")
                    generateRuntimeTypeAppendix("T[]")
                    appendDoc("Array instances can be created using the [arrayOf], [arrayOfNulls] and [emptyArray]")
                    appendDoc("standard library functions.")
                    typeParam("T")
                    noPrimaryConstructor()
                } else {
                    appendDoc("An array of ${typeLower}s.")
                    generateRuntimeTypeAppendix("$typeLower[]")
                    val defaultValue = when (kind) {
                        PrimitiveType.CHAR -> "null char (`\\u0000')"
                        PrimitiveType.BOOLEAN -> "`false`"
                        else -> "zero"
                    }
                    primaryConstructor {
                        appendDoc("Creates a new array of the specified [size], with all elements initialized to $defaultValue.")
                        appendDoc("@throws RuntimeException if the specified [size] is negative.")
                        visibility = MethodVisibility.PUBLIC
                        expectActual = ExpectActualModifier.Inherited(from = this@klass::expectActual)
                        parameter {
                            name = "size"
                            type = PrimitiveType.INT.capitalized
                        }
                    }.modifyPrimaryConstructor()
                }
                appendDoc("")
                appendDoc("See [Kotlin language documentation](https://kotlinlang.org/docs/arrays.html)")
                appendDoc("for more information on arrays.")

                generatePropertiesAndInit()
                generateSecondaryConstructor()
                generateGetSet()
                generateSize()
                generateIterator()

            }.modifyGeneratedClass()
            modifyGeneratedFileAfterClass()
        }

        protected fun FileBuilder.generateArrayIteratorClass() {
            klass {
                visibility = MethodVisibility.PRIVATE
                name = arrayIteratorImplClassName
                if (kind == null) typeParam("T")
                superType(iteratorClassName + if (kind == null) "<T>" else "()")
                primaryConstructor {
                    parameter {
                        visibility = null
                        name = "val array"
                        type = arrayTypeName
                    }
                }
                classBody(
                    """
                    private var index = 0
                    override fun hasNext() = index < array.size
                    override fun next${kind?.capitalized ?: ""}() = if (index < array.size) array[index++] else throw NoSuchElementException("${'$'}index")
                """.trimIndent()
                )
            }
        }

        protected open fun ClassBuilder.generatePropertiesAndInit() {}

        private fun ClassBuilder.generateSecondaryConstructor() {
            secondaryConstructor {
                visibility = MethodVisibility.PUBLIC
                annotations += """Suppress("WRONG_MODIFIER_TARGET")"""
                modifier("inline")
                parameter {
                    name = "size"
                    type = PrimitiveType.INT.capitalized
                }
                parameter {
                    name = "init"
                    type = "(${PrimitiveType.INT.capitalized}) -> $elementTypeName"
                }
                appendDoc("Creates a new array of the specified [size], where each element is calculated by calling the specified")
                appendDoc("[init] function.")
                appendDoc("")
                appendDoc("The function [init] is called for each array element sequentially starting from the first one.")
                appendDoc("It should return the value for an array element given its index.")
                appendDoc("")
                appendDoc("@throws RuntimeException if the specified [size] is negative.")
                noPrimaryConstructorCall()
            }.modifySecondaryConstructor()
        }

        private fun ClassBuilder.generateGetSet() {
            method {
                appendDoc("Returns the array element at the given [index].")
                appendDoc("")
                appendDoc("This method can be called using the index operator:")
                appendDoc("```")
                appendDoc("value = array[index]")
                appendDoc("```")
                appendDoc("")
                appendDoc("If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS")
                appendDoc("where the behavior is unspecified.")
                signature {
                    methodName = "get"
                    isOperator = true
                    parameter {
                        name = "index"
                        type = PrimitiveType.INT.capitalized
                    }
                    returnType = elementTypeName
                }
            }.modifyGetOperator()
            method {
                appendDoc("Sets the array element at the given [index] to the given [value].")
                appendDoc("")
                appendDoc("This method can be called using the index operator:")
                appendDoc("```")
                appendDoc("array[index] = value")
                appendDoc("```")
                appendDoc("")
                appendDoc("If the [index] is out of bounds of this array, throws an [IndexOutOfBoundsException] except in Kotlin/JS")
                appendDoc("where the behavior is unspecified.")
                signature {
                    methodName = "set"
                    isOperator = true
                    parameter {
                        name = "index"
                        type = PrimitiveType.INT.capitalized
                    }
                    parameter {
                        name = "value"
                        type = elementTypeName
                    }
                    returnType = "Unit"
                }
            }.modifySetOperator()
        }

        private fun ClassBuilder.generateSize() {
            property {
                appendDoc("Returns the number of elements in the array.")
                name = "size"
                type = PrimitiveType.INT.capitalized
            }.modifySizeProperty()
        }

        private fun ClassBuilder.generateIterator() {
            method {
                appendDoc("Creates ${if (kind == null) "an [$iteratorClassName]" else "a specialized [$iteratorClassName]"} for iterating over the elements of the array.")
                signature {
                    methodName = "iterator"
                    isOperator = true
                    returnType = iteratorClassName + if (kind == null) "<T>" else ""
                }
            }.modifyIterator()
        }

        protected open fun ClassBuilder.modifyGeneratedClass() {}
        protected open fun PrimaryConstructorBuilder.modifyPrimaryConstructor() {}
        protected open fun FileBuilder.modifyGeneratedFileAfterClass() {}
        protected open fun SecondaryConstructorBuilder.modifySecondaryConstructor() {}
        protected open fun MethodBuilder.modifyGetOperator() {}
        protected open fun MethodBuilder.modifySetOperator() {}
        protected open fun PropertyBuilder.modifySizeProperty() {}
        protected open fun MethodBuilder.modifyIterator() {}
    }

    internal abstract fun arrayBuilder(kind: PrimitiveType?): ArrayBuilder

    private fun FileBuilder.generateClasses() {
        if (primitiveArrays) {
            for (kind in PrimitiveType.entries) {
                with(arrayBuilder(kind)) { generateArrayClass() }
            }
        } else {
            with(arrayBuilder(null)) { generateArrayClass() }
        }
    }

    internal open fun FileBuilder.modifyGeneratedFile() {}
}


class GenerateCommonArrays(writer: PrintWriter, primitiveArrays: Boolean) : GenerateArrays(writer, primitiveArrays) {
    override fun arrayBuilder(kind: PrimitiveType?): ArrayBuilder =
        object : ArrayBuilder(kind, { type -> appendDoc("When targeting the JVM, instances of this class are represented as `$type`.") }) {
            override fun ClassBuilder.modifyGeneratedClass() {
                expectActual = ExpectActualModifier.Expect
            }
        }
}

class GenerateJvmArrays(writer: PrintWriter, primitiveArrays: Boolean) : GenerateArrays(writer, primitiveArrays) {
    override fun FileBuilder.modifyGeneratedFile() {
        annotate("kotlin.internal.JvmBuiltin")
        annotate("kotlin.internal.SuppressBytecodeGeneration")
        suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")
        suppress("PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED")
        suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    }

    override fun arrayBuilder(kind: PrimitiveType?): ArrayBuilder =
        object : ArrayBuilder(kind, { type -> appendDoc("Instances of this class are represented as `$type`.") }) {
            override fun ClassBuilder.modifyGeneratedClass() {
                expectActual = ExpectActualModifier.Actual
            }
        }
}

class GenerateJsArrays(writer: PrintWriter, primitiveArrays: Boolean) : GenerateArrays(writer, primitiveArrays) {
    override fun FileBuilder.modifyGeneratedFile() {
        suppress("UNUSED_PARAMETER")
    }

    override fun arrayBuilder(kind: PrimitiveType?): ArrayBuilder = object : ArrayBuilder(kind) {
        override fun SecondaryConstructorBuilder.modifySecondaryConstructor() {
            annotations.removeAll { it.startsWith("Suppress") }
            annotations += """Suppress("WRONG_MODIFIER_TARGET", "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED")"""
        }

        override fun MethodBuilder.modifyGetOperator() {
            annotations += """Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")"""
        }

        override fun MethodBuilder.modifySetOperator() {
            annotations += """Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")"""
        }

        override fun PropertyBuilder.modifySizeProperty() {
            annotations += """Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")"""
        }

        override fun MethodBuilder.modifyIterator() {
            annotations += """Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")"""
        }
    }
}


class GenerateWasmArrays(writer: PrintWriter, primitiveArrays: Boolean) : GenerateArrays(writer, primitiveArrays) {
    override fun FileBuilder.modifyGeneratedFile() {
        import("kotlin.wasm.internal.*")
        suppress("UNUSED_PARAMETER")
    }

    override fun arrayBuilder(kind: PrimitiveType?): ArrayBuilder = object : ArrayBuilder(kind) {
        private val storageArrayType = when (kind) {
            null -> "WasmAnyArray"
            PrimitiveType.BOOLEAN -> "WasmByteArray"
            else -> "Wasm${kind.capitalized}Array"
        }

        override fun ClassBuilder.modifyGeneratedClass() {
            if (kind == null) {
                primaryConstructor {
                    annotations += "PublishedApi"
                    visibility = MethodVisibility.INTERNAL
                    parameter {
                        name = "size"
                        type = PrimitiveType.INT.capitalized
                    }
                }
            }
        }

        override fun ClassBuilder.generatePropertiesAndInit() {
            property {
                visibility = MethodVisibility.INTERNAL
                expectActual = ExpectActualModifier.Unspecified
                name = "storage"
                type = storageArrayType
            }
            classBody(
                """
                init {
                    if (size < 0) throw IllegalArgumentException("Negative array size")
                    storage = $storageArrayType(size)
                }
                
                @WasmPrimitiveConstructor
                @Suppress("PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED")
                internal constructor(storage: $storageArrayType)
            """.trimIndent()
            )
        }

        override fun SecondaryConstructorBuilder.modifySecondaryConstructor() {
            annotations.removeAll { it.startsWith("Suppress") }
            annotations += """Suppress("WRONG_MODIFIER_TARGET", "TYPE_PARAMETER_AS_REIFIED")"""
            primaryConstructorCall("size")
        }

        override fun MethodBuilder.modifyGetOperator() {
            """
                rangeCheck(index, storage.len())
                ${
                when (kind) {
                    null -> "@Suppress(\"UNCHECKED_CAST\") return storage.get(index) as T"
                    PrimitiveType.BOOLEAN -> "return storage.get(index).reinterpretAsInt().reinterpretAsBoolean()"
                    else -> "return storage.get(index)"
                }
            }
            """.trimIndent().setAsBlockBody()
        }

        override fun MethodBuilder.modifySetOperator() {
            """
                rangeCheck(index, storage.len())
                storage.set(index, value${if (kind == PrimitiveType.BOOLEAN) ".reinterpretAsByte()" else ""})
            """.trimIndent().setAsBlockBody()
        }

        override fun PropertyBuilder.modifySizeProperty() {
            "storage.len()".setAsExpressionGetterBody()
        }

        override fun MethodBuilder.modifyIterator() {
            "$arrayIteratorImplClassName(this)".setAsExpressionBody()
        }

        override fun FileBuilder.modifyGeneratedFileAfterClass() {
            generateArrayIteratorClass()
            if (kind == PrimitiveType.BOOLEAN) {
                method {
                    annotations += "WasmNoOpCast"
                    signature {
                        visibility = MethodVisibility.PRIVATE
                        methodName = "Boolean.reinterpretAsByte"
                        returnType = PrimitiveType.BYTE.capitalized
                    }
                    "implementedAsIntrinsic".setAsExpressionBody()
                }
            }
        }
    }
}

class GenerateNativeArrays(writer: PrintWriter, primitiveArrays: Boolean) : GenerateArrays(writer, primitiveArrays) {
    override fun FileBuilder.modifyGeneratedFile() {
        import("kotlin.native.internal.*")
        import("kotlin.native.internal.escapeAnalysis.Escapes")
        if (!primitiveArrays) {
            import("kotlin.native.internal.escapeAnalysis.PointsTo")
        }
    }

    override fun arrayBuilder(kind: PrimitiveType?): ArrayBuilder = object : ArrayBuilder(kind) {

        override fun ClassBuilder.modifyGeneratedClass() {
            annotations += """ExportTypeInfo("the${arrayClassName}TypeInfo")"""
            method {
                expectActual = ExpectActualModifier.Unspecified
                signature {
                    annotations += """GCUnsafeCall("Kotlin_${arrayClassName}_getArrayLength")"""
                    annotations += """Escapes.Nothing"""
                    methodName = "getArrayLength"
                    visibility = MethodVisibility.PRIVATE
                    isExternal = true
                    returnType = PrimitiveType.INT.capitalized
                }
            }
        }

        override fun PrimaryConstructorBuilder.modifyPrimaryConstructor() {
            annotations += """Suppress("UNUSED_PARAMETER")"""
        }

        override fun ClassBuilder.generatePropertiesAndInit() {
            if (kind == null) {
                classBody(
                    """
                        @PublishedApi
                        @ExportForCompiler
                        internal constructor(@Suppress("UNUSED_PARAMETER") size: Int) {}
                    """.trimIndent()
                )
            }
        }

        override fun FileBuilder.modifyGeneratedFileAfterClass() {
            generateArrayIteratorClass()
        }

        override fun SecondaryConstructorBuilder.modifySecondaryConstructor() {
            annotations.removeAll { it.startsWith("Suppress") }
            annotations += """Suppress("TYPE_PARAMETER_AS_REIFIED", "WRONG_MODIFIER_TARGET")"""

            primaryConstructorCall("size")
            """
                for (i in 0..size - 1) {
                    this[i] = init(i)
                }
            """.trimIndent().setAsBlockBody()
        }

        override fun MethodBuilder.modifyGetOperator() {
            annotations += """GCUnsafeCall("Kotlin_${arrayClassName}_get")"""
            if (kind == null) {
                annotations += """PointsTo(0x000, 0x000, 0x002) // ret -> this.intestines"""
            } else {
                annotations += """Escapes.Nothing"""
            }
            modifySignature {
                isExternal = true
            }
        }

        override fun MethodBuilder.modifySetOperator() {
            annotations += """GCUnsafeCall("Kotlin_${arrayClassName}_set")"""
            if (kind == null) {
                annotations += """PointsTo(0x0300, 0x0000, 0x0000, 0x0000) // this.intestines -> value"""
            } else {
                annotations += """Escapes.Nothing"""
            }
            modifySignature {
                isExternal = true
            }
        }

        override fun PropertyBuilder.modifySizeProperty() {
            "getArrayLength()".setAsExpressionGetterBody()
        }

        override fun MethodBuilder.modifyIterator() {
            "$arrayIteratorImplClassName(this)".setAsExpressionBody()
        }
    }
}

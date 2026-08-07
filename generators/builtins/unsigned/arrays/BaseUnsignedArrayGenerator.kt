/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package unsigned.arrays

import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ClassModifier
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ExpectActualModifier
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.FileBuilder
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.MethodVisibility
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.file
import org.jetbrains.kotlin.generators.builtins.unsigned.INLINE_ONLY
import org.jetbrains.kotlin.generators.builtins.unsigned.PUBLISHED_API
import java.io.PrintWriter

abstract class BaseUnsignedArrayGenerator(
    val type: UnsignedType,
    private val out: PrintWriter,
    private val expectActualModifier: ExpectActualModifier,
) : BuiltInsGenerator {
    private val elementType = type.capitalized
    private val arrayType = elementType + "Array"
    private val arrayTypeOf = elementType.lowercase() + "ArrayOf"
    private val storageElementType = type.asSigned.capitalized
    private val storageArrayType = storageElementType + "Array"

    open val extraImports = emptySet<String>()
    open val extraClassAnnotations = emptySet<String>()

    open fun operatorGetBody(): String =
        "storage[index].to$elementType()"

    open fun operatorSetBody(): String =
        "storage[index] = value.to$storageElementType()"

    open fun containsBody(): String =
        "storage.contains(element.to$storageElementType())"

    open fun containsAllBody(): String =
        "(elements as Collection<*>).all { it is $elementType && storage.contains(it.to$storageElementType()) }"

    open fun isEmptyBody(): String =
        "this.storage.size == 0"

    open fun sizeGetterBody(): String =
        "storage.size"

    open fun iteratorBody(): String =
        "Iterator(storage)"

    open fun iteratorHasNextBody(): String =
        "index < array.size"

    open fun iteratorNextBody(): String =
        "if (index < array.size) array[index++].to$elementType() else throw NoSuchElementException(index.toString())"

    open fun factoryMethodBody(): String =
        "$arrayType($storageArrayType(size) { index -> init(index).to$storageElementType() })"

    override fun generate() {
        out.print(buildFile().build())
    }

    private fun buildFile(): FileBuilder = file(this::class) {
        extraImports.forEach(::import)

        klass {
            expectActual = expectActualModifier
            annotations += "SinceKotlin(\"1.3\")"
            annotations += "ExperimentalUnsignedTypes"
            annotations += extraClassAnnotations
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    primaryConstructorCall("$storageArrayType(size)")
                } else {
                    noPrimaryConstructorCall()
                }
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    operatorGetBody().setAsBody()
                }
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    operatorSetBody().setAsBlockBody()
                }
            }

            property {
                appendDoc("Returns the number of elements in the array.")
                modifier("override")
                name = "size"
                type = "Int"

                if (expectActualModifier != ExpectActualModifier.Expect) {
                    sizeGetterBody().setAsExpressionGetterBody()
                }
            }

            method {
                appendDoc("Creates an iterator over the elements of the array.")
                signature {
                    isOverride = true
                    isOperator = true
                    methodName = "iterator"
                    returnType = "kotlin.collections.Iterator<$elementType>"
                }

                if (expectActualModifier != ExpectActualModifier.Expect) {
                    iteratorBody().setAsBody()
                }
            }

            if (expectActualModifier != ExpectActualModifier.Expect) {
                klass {
                    visibility = MethodVisibility.PRIVATE
                    name = "Iterator"
                    superType("kotlin.collections.Iterator<$elementType>")
                    expectActual = ExpectActualModifier.Unspecified
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
                        iteratorHasNextBody().setAsBody()
                    }
                    method {
                        signature {
                            isOverride = true
                            returnType = elementType
                            methodName = "next"
                        }
                        iteratorNextBody().setAsBody()
                    }
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
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    containsBody().setAsBody()
                }
            }

            method {
                signature {
                    isOverride = true
                    visibility = null
                    methodName = "containsAll"
                    parameter { name = "elements"; type = "Collection<$elementType>" }
                    returnType = "Boolean"
                }
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    containsAllBody().setAsBody()
                }
            }

            method {
                signature {
                    isOverride = true
                    visibility = null
                    methodName = "isEmpty"
                    returnType = "Boolean"
                }
                if (expectActualModifier != ExpectActualModifier.Expect) {
                    isEmptyBody().setAsBody()
                }
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
            expectActual = expectActualModifier
            signature {
                isInline = true
                methodName = arrayType
                parameter { name = "size"; type = "Int" }
                parameter { name = "init"; type = "(Int) -> $elementType" }
                returnType = arrayType
            }
            if (expectActualModifier != ExpectActualModifier.Expect) {
                factoryMethodBody().setAsBody()
            }
        }

        // It seems like this is the only common method
        if (expectActualModifier == ExpectActualModifier.Expect) {
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
}

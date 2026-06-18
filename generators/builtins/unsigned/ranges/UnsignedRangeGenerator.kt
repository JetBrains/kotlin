/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package unsigned.ranges

import org.jetbrains.kotlin.generators.builtins.UnsignedType
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.BuiltInsGenerator
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.ClassModifier
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.END_LINE
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.FileBuilder
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.MethodVisibility
import org.jetbrains.kotlin.generators.builtins.numbers.primitives.file
import java.io.PrintWriter

class UnsignedRangeGenerator(val type: UnsignedType, private val out: PrintWriter) : BuiltInsGenerator {
    private val elementType = type.capitalized
    private val signedType = type.asSigned.capitalized
    private val stepType = signedType
    private val stepMinValue = "$stepType.MIN_VALUE"

    private fun hashCodeConversion(name: String, isSigned: Boolean = false) =
        if (type == UnsignedType.ULONG) "($name xor ($name ${if (isSigned) "u" else ""}shr 32))" else name

    override fun generate() {
        out.print(buildFile().build())
    }

    private fun buildFile(): FileBuilder = file(this::class, packageName = "kotlin.ranges") {
        import("kotlin.internal.*")

        generateRange()
        generateProgression()
        generateProgressionIterator()
    }

    private fun FileBuilder.generateRange() {
        klass {
            appendDoc("A range of values of type `$elementType`.")
            annotations += "SinceKotlin(\"1.5\")"
            name = "${elementType}Range"
            primaryConstructor {
                visibility = null
                parameter { name = "start"; type = elementType }
                parameter { name = "endInclusive"; type = elementType }
            }
            superType("${elementType}Progression(start, endInclusive, 1)")
            superType("ClosedRange<$elementType>")
            superType("OpenEndRange<$elementType>")

            property {
                visibility = null
                modifier("override")
                name = "start"
                type = elementType
                "first".setAsExpressionGetterBody()
            }
            property {
                visibility = null
                modifier("override")
                name = "endInclusive"
                type = elementType
                "last".setAsExpressionGetterBody()
            }
            property {
                visibility = null
                modifier("override")
                annotations += "Deprecated(\"Can throw an exception when it's impossible to represent the value with $elementType type, for example, when the range includes MAX_VALUE. It's recommended to use 'endInclusive' property that doesn't throw.\")"
                annotations += "SinceKotlin(\"1.9\")"
                annotations += "WasExperimental(ExperimentalStdlibApi::class)"
                name = "endExclusive"
                type = elementType
                """
                        if (last == $elementType.MAX_VALUE) error("Cannot return the exclusive upper bound of a range that includes MAX_VALUE.")
                        return last + 1u
                        """.trimIndent().setAsBlockGetterBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "contains"
                    parameter { name = "value"; type = elementType }
                    returnType = "Boolean"
                }
                "first <= value && value <= last".setAsExpressionBody()
            }

            method {
                appendDoc(
                    """
                    Checks if the range is empty.

                    The range is empty if its start value is greater than the end value.
                    """.trimIndent()
                )
                signature {
                    isOverride = true
                    methodName = "isEmpty"
                    returnType = "Boolean"
                }
                "first > last".setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "equals"
                    parameter { name = "other"; type = "Any?" }
                    returnType = "Boolean"
                }
                ("other is ${elementType}Range && (isEmpty() && other.isEmpty() ||" + END_LINE +
                        "        first == other.first && last == other.last)").setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "hashCode"
                    returnType = "Int"
                }
                "if (isEmpty()) -1 else (31 * ${hashCodeConversion("first")}.toInt() + ${hashCodeConversion("last")}.toInt())".setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "toString"
                    returnType = "String"
                }
                "\"\$first..\$last\"".setAsExpressionBody()
            }

            companionObject {
                property {
                    appendDoc("An empty range of values of type $elementType.")
                    name = "EMPTY"
                    type = "${elementType}Range"
                    value = "${elementType}Range($elementType.MAX_VALUE, $elementType.MIN_VALUE)"
                }
            }
        }
    }

    private fun FileBuilder.generateProgression() {
        klass {
            appendDoc("A progression of values of type `$elementType`.")
            annotations += "SinceKotlin(\"1.5\")"
            annotations += "Suppress(\"REDUNDANT_CALL_OF_CONVERSION_METHOD\")"
            name = "${elementType}Progression"
            modifier(ClassModifier.OPEN)
            primaryConstructor {
                visibility = MethodVisibility.INTERNAL
                parameter { name = "start"; type = elementType }
                parameter { name = "endInclusive"; type = elementType }
                parameter { name = "step"; type = stepType }
            }
            superType("Iterable<$elementType>")
            initBlock(
                """
                if (step == 0.to$stepType()) throw kotlin.IllegalArgumentException("Step must be non-zero.")
                if (step == $stepMinValue) throw kotlin.IllegalArgumentException("Step must be greater than $stepMinValue to avoid overflow on negation.")
                """.trimIndent()
            )

            property {
                appendDoc("The first element in the progression.")
                name = "first"
                type = elementType
                value = "start"
            }
            property {
                appendDoc("The last element in the progression.")
                name = "last"
                type = elementType
                value = "getProgressionLastElement(start, endInclusive, step)"
            }
            property {
                appendDoc("The step of the progression.")
                name = "step"
                type = stepType
                value = "step"
            }

            method {
                signature {
                    isFinal = true
                    isOverride = true
                    methodName = "iterator"
                    returnType = "Iterator<$elementType>"
                }
                "${elementType}ProgressionIterator(first, last, step)".setAsExpressionBody()
            }

            method {
                appendDoc(
                    """
                    Checks if the progression is empty.

                    Progression with a positive step is empty if its first element is greater than the last element.
                    Progression with a negative step is empty if its first element is less than the last element.
                    """.trimIndent()
                )
                signature {
                    isOpen = true
                    methodName = "isEmpty"
                    returnType = "Boolean"
                }
                "if (step > 0) first > last else first < last".setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "equals"
                    parameter { name = "other"; type = "Any?" }
                    returnType = "Boolean"
                }
                ("other is ${elementType}Progression && (isEmpty() && other.isEmpty() ||" + END_LINE +
                        "        first == other.first && last == other.last && step == other.step)").setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "hashCode"
                    returnType = "Int"
                }
                ("if (isEmpty()) -1 else (31 * (31 * ${hashCodeConversion("first")}.toInt() + ${hashCodeConversion("last")}.toInt()) + " +
                        "${hashCodeConversion("step", isSigned = true)}.toInt())").setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "toString"
                    returnType = "String"
                }
                "if (step > 0) \"\$first..\$last step \$step\" else \"\$first downTo \$last step \${-step}\"".setAsExpressionBody()
            }

            companionObject {
                method {
                    appendDoc(
                        """
                        Creates ${elementType}Progression within the specified bounds of a closed range.

                        The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
                        In order to go backwards the [step] must be negative.

                        [step] must be greater than `$stepMinValue` and not equal to zero.
                        """.trimIndent()
                    )
                    signature {
                        methodName = "fromClosedRange"
                        parameter { name = "rangeStart"; type = elementType }
                        parameter { name = "rangeEnd"; type = elementType }
                        parameter { name = "step"; type = stepType }
                        returnType = "${elementType}Progression"
                    }
                    "${elementType}Progression(rangeStart, rangeEnd, step)".setAsExpressionBody()
                }
            }
        }
    }

    private fun FileBuilder.generateProgressionIterator() {
        klass {
            appendDoc(
                """
                An iterator over a progression of values of type `$elementType`.
                @property step the number by which the value is incremented on each step.
                """.trimIndent()
            )
            annotations += "SinceKotlin(\"1.3\")"
            visibility = MethodVisibility.PRIVATE
            name = "${elementType}ProgressionIterator"
            primaryConstructor {
                visibility = null
                parameter { name = "first"; type = elementType }
                parameter { name = "last"; type = elementType }
                parameter { name = "step"; type = stepType }
            }
            superType("Iterator<$elementType>")

            property {
                visibility = MethodVisibility.PRIVATE
                name = "finalElement"
                type = elementType
                value = "last"
            }
            property {
                visibility = MethodVisibility.PRIVATE
                isMutable = true
                name = "hasNext"
                type = "Boolean"
                value = "if (step > 0) first <= last else first >= last"
            }
            property {
                visibility = MethodVisibility.PRIVATE
                additionalComments = "use 2-complement math for negative steps"
                name = "step"
                type = elementType
                value = "step.to$elementType()"
            }
            property {
                visibility = MethodVisibility.PRIVATE
                isMutable = true
                name = "next"
                type = elementType
                value = "if (hasNext) first else finalElement"
            }

            method {
                signature {
                    isOverride = true
                    methodName = "hasNext"
                    returnType = "Boolean"
                }
                "hasNext".setAsExpressionBody()
            }

            method {
                signature {
                    isOverride = true
                    methodName = "next"
                    returnType = elementType
                }
                """
                val value = next
                if (value == finalElement) {
                    if (!hasNext) throw kotlin.NoSuchElementException()
                    hasNext = false
                } else {
                    next += step
                }
                return value
                """.trimIndent().setAsBlockBody()
            }
        }
    }
}

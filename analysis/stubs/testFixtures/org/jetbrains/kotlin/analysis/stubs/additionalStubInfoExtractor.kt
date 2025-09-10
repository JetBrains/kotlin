/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.stubs.impl.*
import java.lang.reflect.Modifier

internal fun extractAdditionalStubInfo(stub: KotlinFileStubImpl): String = prettyPrint {
    extractAdditionInfo(stub)
}

private fun PrettyPrinter.extractAdditionInfo(stub: StubElement<*>) {
    // Nodes are stored in the form "NodeType:Node" and have too much repeating information for Kotlin stubs
    // Remove all repeating information (See KotlinStubBaseImpl.toString())
    val adjustedStubText = stub.toString().substringAfter(STUB_TO_STRING_PREFIX).replace(", [", "[")
    appendLine(adjustedStubText)

    withIndent {
        val additionalProperties = stub::class.java
            .declaredMethods
            // All "public" information from stub interfaces is already rendered via regular toString()
            .filter { it.parameterTypes.isEmpty() && Modifier.isFinal(it.modifiers) }
            .sortedBy { it.name }

        for (method in additionalProperties) {
            val value = method(stub) ?: continue

            val methodName = method.name
            val name = if (methodName.startsWith("get")) {
                methodName.substring(3).replaceFirstChar(Char::lowercaseChar)
            } else {
                methodName
            }

            append(name).append(": ")
            appendValue(value)
            appendLine()
        }

        for (child in stub.childrenStubs) {
            extractAdditionInfo(child)
        }
    }
}

private fun PrettyPrinter.appendValue(value: Any?) {
    when (value) {
        is Map<*, *> -> appendValue(value.entries)
        is Collection<*> -> when (value.size) {
            0 -> append("[ ]")
            1 -> {
                append("[ ")
                withIndent {
                    appendValue(value.single())
                }
                append(" ]")
            }

            else -> printCollection(value, separator = "\n", prefix = "[\n", postfix = "\n]") {
                withIndent {
                    appendValue(it)
                }
            }
        }

        is Map.Entry<*, *> -> {
            appendValue(value.key)
            append(" -> ")
            appendValue(value.value)
        }

        is KotlinTypeBean -> appendTypeInfo(value)
        is Name -> append(value.asString())
        is Enum<*> -> append(value.name)
        is String -> append("\"").append(value).append("\"")
        is FqName -> append(value.asString())
        is KtContractDescriptionElement<*, *> -> {
            append("effect:")
            @Suppress("UNCHECKED_CAST")
            (value as KtContractDescriptionElement<KotlinTypeBean, Nothing?>).accept(KotlinContractRenderer(this), null)
        }

        null -> append("null")
        is ConstantValue<*>, is KotlinStubOrigin -> append(value.toString())
        else -> error("Unsupported type: ${value::class}")
    }
}

private fun PrettyPrinter.appendTypeInfo(typeBean: KotlinTypeBean) {
    when (typeBean) {
        is KotlinClassTypeBean -> {
            append(typeBean.classId.asFqNameString())
            val arguments = typeBean.arguments
            if (arguments.isNotEmpty()) {
                append("<")
                arguments.forEachIndexed { index, arg ->
                    if (index > 0) append(", ")
                    if (arg.projectionKind != KtProjectionKind.NONE) {
                        append(arg.projectionKind.name)
                    }
                    if (arg.projectionKind != KtProjectionKind.STAR) {
                        appendTypeInfo(arg.type!!)
                    }
                }
                append(">")
            }
            if (typeBean.nullable) {
                append("?")
            }

            val abbreviatedType = typeBean.abbreviatedType
            if (abbreviatedType != null) {
                append(" (abbreviatedType: ")
                appendTypeInfo(abbreviatedType)
                append(")")
            }
        }
        is KotlinTypeParameterTypeBean -> {
            append(typeBean.typeParameterName)
            if (typeBean.nullable) {
                append("?")
            }
            if (typeBean.definitelyNotNull) {
                append(" & Any")
            }
        }

        is KotlinFlexibleTypeBean -> {
            appendTypeInfo(typeBean.lowerBound)
            append(" .. ")
            appendTypeInfo(typeBean.upperBound)
        }
    }
}

class KotlinContractRenderer(
    private val printer: PrettyPrinter,
) : KtContractDescriptionVisitor<Unit, Nothing?, KotlinTypeBean, Nothing?>() {
    override fun visitConditionalEffectDeclaration(
        conditionalEffect: KtConditionalEffectDeclaration<KotlinTypeBean, Nothing?>,
        data: Nothing?,
    ) {
        conditionalEffect.effect.accept(this, data)
        printer.append(" -> ")
        conditionalEffect.condition.accept(this, data)
    }

    override fun visitConditionalReturnsDeclaration(
        conditionalEffect: KtConditionalReturnsDeclaration<KotlinTypeBean, Nothing?>,
        data: Nothing?,
    ) {
        conditionalEffect.argumentsCondition.accept(this, data)
        printer.append(" -> ")
        conditionalEffect.returnsEffect.accept(this, data)
    }

    override fun visitHoldsInEffectDeclaration(
        holdsInEffect: KtHoldsInEffectDeclaration<KotlinTypeBean, Nothing?>,
        data: Nothing?,
    ) {
        holdsInEffect.argumentsCondition.accept(this, data)
        printer.append(" HoldsIn(")
        holdsInEffect.valueParameterReference.accept(this, data)
        printer.append(")")
    }

    override fun visitReturnsEffectDeclaration(returnsEffect: KtReturnsEffectDeclaration<KotlinTypeBean, Nothing?>, data: Nothing?) {
        printer.append("Returns(")
        returnsEffect.value.accept(this, data)
        printer.append(")")
    }

    override fun visitCallsEffectDeclaration(callsEffect: KtCallsEffectDeclaration<KotlinTypeBean, Nothing?>, data: Nothing?) {
        printer.append("CallsInPlace(")
        callsEffect.valueParameterReference.accept(this, data)
        printer.append(", ${callsEffect.kind})")
    }

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: KtBinaryLogicExpression<KotlinTypeBean, Nothing?>,
        data: Nothing?,
    ) {
        binaryLogicExpression.left.accept(this, data)
        printer.append(" ${binaryLogicExpression.kind.token} ")
        binaryLogicExpression.right.accept(this, data)
    }

    override fun visitLogicalNot(logicalNot: KtLogicalNot<KotlinTypeBean, Nothing?>, data: Nothing?) {
        logicalNot.arg.accept(this, data)
    }

    override fun visitIsInstancePredicate(isInstancePredicate: KtIsInstancePredicate<KotlinTypeBean, Nothing?>, data: Nothing?) {
        isInstancePredicate.arg.accept(this, data)
        printer.append(" ${if (isInstancePredicate.isNegated) "!" else ""}is ${isInstancePredicate.type}")
    }

    override fun visitIsNullPredicate(isNullPredicate: KtIsNullPredicate<KotlinTypeBean, Nothing?>, data: Nothing?) {
        isNullPredicate.arg.accept(this, data)
        printer.append(" ${if (isNullPredicate.isNegated) "!=" else "=="} null")
    }

    override fun visitConstantDescriptor(constantReference: KtConstantReference<KotlinTypeBean, Nothing?>, data: Nothing?) {
        printer.append(constantReference.name)
    }

    override fun visitValueParameterReference(
        valueParameterReference: KtValueParameterReference<KotlinTypeBean, Nothing?>,
        data: Nothing?,
    ) {
        printer.append("param(").append(valueParameterReference.parameterIndex.toString()).append(")")
    }
}

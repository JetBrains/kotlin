/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.stubs.impl.*

internal fun extractAdditionalStubInfo(stub: KotlinFileStubImpl): String = prettyPrint {
    extractAdditionInfo(stub)
}

private fun PrettyPrinter.extractAdditionInfo(stub: StubElement<*>) {
    append(stub.toString())
    when (stub) {
        is KotlinUserTypeStubImpl -> {
            val upperBound = stub.upperBound
            if (upperBound != null) {
                append("    upperBound: ")
                appendTypeInfo(upperBound)
            }

            val abbreviatedType = stub.abbreviatedType
            if (abbreviatedType != null) {
                append("    abbreviatedType: ")
                appendTypeInfo(abbreviatedType)
            }
        }

        is KotlinFunctionTypeStubImpl -> {
            val abbreviatedType = stub.abbreviatedType
            if (abbreviatedType != null) {
                append("    abbreviatedType: ")
                appendTypeInfo(abbreviatedType)
            }
        }

        is KotlinFunctionStubImpl -> {
            val contract = stub.contract
            if (contract != null) {
                withIndent {
                    for (element in contract) {
                        appendLine()
                        append("effect:")
                        element.accept(KotlinContractRenderer(this), null)
                    }
                }
            }
        }
        is KotlinPropertyStubImpl -> {
            val initializer = stub.constantInitializer
            if (initializer != null) {
                withIndent {
                    appendLine()
                    append("initializer: $initializer")
                }
            }
        }
        is KotlinAnnotationEntryStubImpl -> {
            val arguments = stub.valueArguments
            if (arguments != null) {
                withIndent {
                    appendLine()
                    append("valueArguments: ")
                    withIndent {
                        arguments.entries.joinTo(this, ", ", "(", ")") { "${it.key.asString()} = ${it.value}" }
                    }
                }
            }
        }
        is KotlinParameterStubImpl -> {
            stub.functionTypeParameterName?.let { append("   paramNameByAnnotation: ").append(it) }
        }
        is KotlinClassStubImpl -> {
            stub.valueClassRepresentation?.let { append("   valueClassRepresentation: ").append(it.toString()) }
        }
    }
    for (child in stub.childrenStubs) {
        withIndent {
            appendLine()
            extractAdditionInfo(child)
        }
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

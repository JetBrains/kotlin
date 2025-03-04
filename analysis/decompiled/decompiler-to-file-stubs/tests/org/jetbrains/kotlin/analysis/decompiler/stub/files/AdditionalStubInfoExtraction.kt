/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.files

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.stubs.impl.*

fun extractAdditionalStubInfo(stub: StubElement<*>): String {
    val builder = StringBuilder()
    extractAdditionInfo(stub, builder, 0)
    return builder.toString()
}

private fun extractAdditionInfo(stub: StubElement<*>, builder: StringBuilder, level: Int) {
    builder.append(stub.toString())
    when (stub) {
        is KotlinUserTypeStubImpl -> {
            val upperBound = stub.upperBound
            if (upperBound != null) {
                builder.append("    upperBound: ")
                appendTypeInfo(builder, upperBound)
            }

            val abbreviatedType = stub.abbreviatedType
            if (abbreviatedType != null) {
                builder.append("    abbreviatedType: ")
                appendTypeInfo(builder, abbreviatedType)
            }
        }
        is KotlinFunctionTypeStubImpl -> {
            val abbreviatedType = stub.abbreviatedType
            if (abbreviatedType != null) {
                builder.append("    abbreviatedType: ")
                appendTypeInfo(builder, abbreviatedType)
            }
        }
        is KotlinFunctionStubImpl -> {
            val contract = stub.contract
            if (contract != null) {
                for (element in contract) {
                    builder.append("\n" + "  ".repeat(level)).append("effect:")
                    element.accept(KotlinContractRenderer(builder), null)
                }
            }
        }
        is KotlinPropertyStubImpl -> {
            val initializer = stub.constantInitializer
            if (initializer != null) {
                builder.append("\n").append("  ".repeat(level)).append("initializer: ${initializer.value}")
            }
        }
        is KotlinAnnotationEntryStubImpl -> {
            val arguments = stub.valueArguments
            if (arguments != null) {
                builder
                    .append("\n")
                    .append("  ".repeat(level))
                    .append("valueArguments: ")
                    .append(arguments.entries.joinToString(", ", "(", ")") { "${it.key.asString()} = ${it.value}" })
            }
        }
        is KotlinParameterStubImpl -> {
            stub.functionTypeParameterName?.let { builder.append("   paramNameByAnnotation: ").append(it) }
        }
        is KotlinClassStubImpl -> {
            stub.valueClassRepresentation?.let { builder.append("   valueClassRepresentation: ").append(it) }
        }
    }
    for (child in stub.childrenStubs) {
        builder.append("\n").append("  ".repeat(level))
        extractAdditionInfo(child, builder, level + 1)
    }
}

private fun appendTypeInfo(builder: StringBuilder, typeBean: KotlinTypeBean) {
    when (typeBean) {
        is KotlinClassTypeBean -> {
            builder.append(typeBean.classId.asFqNameString())
            val arguments = typeBean.arguments
            if (arguments.isNotEmpty()) {
                builder.append("<")
                arguments.forEachIndexed { index, arg ->
                    if (index > 0) builder.append(", ")
                    if (arg.projectionKind != KtProjectionKind.NONE) {
                        builder.append(arg.projectionKind.name)
                    }
                    if (arg.projectionKind != KtProjectionKind.STAR) {
                        appendTypeInfo(builder, arg.type!!)
                    }
                }
                builder.append(">")
            }
            if (typeBean.nullable) {
                builder.append("?")
            }

            val abbreviatedType = typeBean.abbreviatedType
            if (abbreviatedType != null) {
                builder.append(" (abbreviatedType: ")
                appendTypeInfo(builder, abbreviatedType)
                builder.append(")")
            }
        }
        is KotlinTypeParameterTypeBean -> {
            builder.append(typeBean.typeParameterName)
            if (typeBean.nullable) {
                builder.append("?")
            }
            if (typeBean.definitelyNotNull) {
                builder.append(" & Any")
            }
        }

        is KotlinFlexibleTypeBean -> {
            appendTypeInfo(builder, typeBean.lowerBound)
            builder.append(" .. ")
            appendTypeInfo(builder, typeBean.upperBound)
        }
    }
}

class KotlinContractRenderer(private val buffer: StringBuilder) : KtContractDescriptionVisitor<Unit, Nothing?, KotlinTypeBean, Nothing?>() {
    override fun visitConditionalEffectDeclaration(conditionalEffect: KtConditionalEffectDeclaration<KotlinTypeBean, Nothing?>, data: Nothing?) {
        conditionalEffect.effect.accept(this, data)
        buffer.append(" -> ")
        conditionalEffect.condition.accept(this, data)
    }

    override fun visitConditionalReturnsDeclaration(
        conditionalEffect: KtConditionalReturnsDeclaration<KotlinTypeBean, Nothing?>,
        data: Nothing?,
    ) {
        conditionalEffect.argumentsCondition.accept(this, data)
        buffer.append(" -> ")
        conditionalEffect.returnsEffect.accept(this, data)
    }

    override fun visitReturnsEffectDeclaration(returnsEffect: KtReturnsEffectDeclaration<KotlinTypeBean, Nothing?>, data: Nothing?) {
        buffer.append("Returns(")
        returnsEffect.value.accept(this, data)
        buffer.append(")")
    }

    override fun visitCallsEffectDeclaration(callsEffect: KtCallsEffectDeclaration<KotlinTypeBean, Nothing?>, data: Nothing?) {
        buffer.append("CallsInPlace(")
        callsEffect.valueParameterReference.accept(this, data)
        buffer.append(", ${callsEffect.kind})")
    }

    override fun visitLogicalBinaryOperationContractExpression(binaryLogicExpression: KtBinaryLogicExpression<KotlinTypeBean, Nothing?>, data: Nothing?) {
        binaryLogicExpression.left.accept(this, data)
        buffer.append(" ${binaryLogicExpression.kind.token} ")
        binaryLogicExpression.right.accept(this, data)
    }

    override fun visitLogicalNot(logicalNot: KtLogicalNot<KotlinTypeBean, Nothing?>, data: Nothing?) {
        logicalNot.arg.accept(this, data)
    }

    override fun visitIsInstancePredicate(isInstancePredicate: KtIsInstancePredicate<KotlinTypeBean, Nothing?>, data: Nothing?) {
        isInstancePredicate.arg.accept(this, data)
        buffer.append(" ${if (isInstancePredicate.isNegated) "!" else ""}is ${isInstancePredicate.type}")
    }

    override fun visitIsNullPredicate(isNullPredicate: KtIsNullPredicate<KotlinTypeBean, Nothing?>, data: Nothing?) {
        isNullPredicate.arg.accept(this, data)
        buffer.append(" ${if (isNullPredicate.isNegated) "!=" else "=="} null")
    }

    override fun visitConstantDescriptor(constantReference: KtConstantReference<KotlinTypeBean, Nothing?>, data: Nothing?) {
        buffer.append(constantReference.name)
    }

    override fun visitValueParameterReference(valueParameterReference: KtValueParameterReference<KotlinTypeBean, Nothing?>, data: Nothing?) {
        buffer.append("param(").append(valueParameterReference.parameterIndex).append(")")
    }
}

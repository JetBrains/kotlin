/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.nextOrNull
import org.jetbrains.kotlin.utils.addToStdlib.withSeenElementsCounting

open class ConeTypeRenderer(
    private val attributeRenderer: ConeAttributeRenderer = ConeAttributeRenderer.ToString,
    private var renderCapturedDetails: Boolean = false,
) {
    lateinit var builder: StringBuilder
    lateinit var idRenderer: ConeIdRenderer

    open fun renderAsPossibleFunctionType(
        type: ConeKotlinType,
        functionClassKindExtractor: (ConeKotlinType) -> FunctionTypeKind?,
        renderType: ConeTypeProjection.() -> Unit = { render() }
    ) {
        val kind = functionClassKindExtractor(type)
        if (kind?.isReflectType != false) {
            type.renderType()
            return
        }

        type.renderNonCompilerAttributes()

        if (type.isMarkedNullable) {
            builder.append("(")
        }
        kind.prefixForTypeRender?.let {
            builder.append(it)
            builder.append(" ")
        }

        var contextParameters: List<ConeTypeProjection> = emptyList()
        var receiverParameter: ConeTypeProjection? = null
        var regularParameters: List<ConeTypeProjection> = type.typeArguments.asList()

        val numberOfContextParameters = type.contextParameterNumberForFunctionType
        if (numberOfContextParameters > 0 && regularParameters.size >= numberOfContextParameters + 1) {
            contextParameters = regularParameters.subList(0, numberOfContextParameters)
            regularParameters = regularParameters.subList(numberOfContextParameters, regularParameters.size)
        }

        if (type.isExtensionFunctionType && regularParameters.size >= 2 && regularParameters.first() != ConeStarProjection) {
            receiverParameter = regularParameters.first()
            regularParameters = regularParameters.subList(1, regularParameters.size)
        }

        if (contextParameters.isNotEmpty()) {
            builder.append("context(")
            for ((index, contextParameter) in contextParameters.withIndex()) {
                if (index != 0) {
                    builder.append(", ")
                }

                contextParameter.render()
            }
            builder.append(") ")
        }

        val arguments = regularParameters.subList(0, regularParameters.size - 1)
        val returnType = regularParameters.last()
        if (receiverParameter != null) {
            receiverParameter.render()
            builder.append(".")
        }
        builder.append("(")
        for ((index, argument) in arguments.withIndex()) {
            if (index != 0) {
                builder.append(", ")
            }
            argument.render()
        }
        builder.append(") -> ")
        returnType.render()
        if (type.isMarkedNullable) {
            builder.append(")?")
        }
    }

    fun render(
        type: ConeKotlinType,
        nullabilityMarker: String = if (type !is ConeFlexibleType && type.isMarkedNullable) "?" else "",
    ) {
        if (type !is ConeFlexibleType && type !is ConeDefinitelyNotNullType && type.classId?.isFunctionType() != true) {
            // We don't render attributes for flexible/definitely not null/extension function types here,
            // because bounds duplicate these attributes often
            type.renderAttributes()
        }
        when (type) {
            is ConeDefinitelyNotNullType -> {
                render(type)
            }

            is ConeIntersectionType -> {
                render(type)
            }

            is ConeDynamicType -> {
                builder.append("dynamic")
            }

            is ConeFlexibleType -> {
                render(type)
            }

            is ConeSimpleKotlinType -> when {
                type is ConeClassLikeType && type.classId.isFunctionType() -> {
                    renderFunctionType(type, nullabilityMarker)
                    return
                }

                else -> {
                    renderSimpleType(type, nullabilityMarker)
                    return
                }
            }
        }
        builder.append(nullabilityMarker)
    }

    private fun renderFunctionType(type: ConeClassLikeType, nullabilityMarker: String = "") {
        if (nullabilityMarker != "") {
            builder.append('(')
        }
        if (type.classId.isSuspendFunctionType()) {
            builder.append("suspend ")
        }
        val arguments = type.typeArguments.iterator().withSeenElementsCounting()

        if (type.hasContextParameters) {
            builder.append("context(")
            repeat(type.contextParameterNumberForFunctionType - 1) {
                arguments.nextOrNull()?.render()
                builder.append(", ")
            }
            arguments.nextOrNull()?.render()
            builder.append(") ")
        }

        if (type.isExtensionFunctionType) {
            arguments.nextOrNull()?.let { next ->
                if (couldBenefitFromParenthesizing(next)) {
                    builder.append('(')
                }
                next.render()
                if (couldBenefitFromParenthesizing(next)) {
                    builder.append(')')
                }
            }
            builder.append(".")
        }

        builder.append('(')
        while (arguments.numberOfElementsSeen < type.typeArguments.size - 2) {
            arguments.nextOrNull()?.render()
            builder.append(", ")
        }
        if (arguments.numberOfElementsSeen == type.typeArguments.size - 2) {
            arguments.nextOrNull()?.render()
        }
        builder.append(") -> ")
        arguments.nextOrNull()?.render()

        if (nullabilityMarker != "") {
            builder.append(')')
            builder.append(nullabilityMarker)
        }
        return
    }

    protected open fun couldBenefitFromParenthesizing(projection: ConeTypeProjection): Boolean =
        // For non-invariant projections, e.g., `(in T).() -> Unit`
        projection !is ConeKotlinType
                // For `(T & Any).() -> Unit`
                || projection is ConeDefinitelyNotNullType
                // For `(() -> Unit)?.() -> Unit` and `(() -> Unit).() -> Unit`
                || projection.classId?.isFunctionType() == true && !projection.isMarkedNullable

    private fun ClassId.isFunctionType(): Boolean = isNonSuspendFunctionType() || isSuspendFunctionType()

    private fun ClassId.isNonSuspendFunctionType(): Boolean =
        this.asString().removePrefix(StandardClassIds.Function.asString()).toIntOrNull() != null

    private fun ClassId.isSuspendFunctionType(): Boolean =
        this.asString().removePrefix(StandardClassIds.SuspendFunction.asString()).toIntOrNull() != null

    protected open fun renderSimpleType(type: ConeSimpleKotlinType, nullabilityMarker: String) {
        val hasTypeArguments = type is ConeClassLikeType && type.typeArguments.isNotEmpty()
        renderConstructor(type.getConstructor())
        if (hasTypeArguments) {
            type.renderTypeArguments()
        }

        builder.append(nullabilityMarker)
    }

    fun renderConstructor(constructor: TypeConstructorMarker) {
        require(constructor is ConeTypeConstructorMarker)
        when (constructor) {
            is ConeTypeVariableTypeConstructor -> {
                builder.append("TypeVariable(")
                builder.append(constructor.name)
                builder.append(")")
            }

            is ConeCapturedTypeConstructor -> {
                builder.append("CapturedType(")
                constructor.projection.render()
                builder.append(")")
                if (renderCapturedDetails) {
                    builder.append(
                        " with lowerType=${constructor.lowerType?.let(::render)}, supertypes=["
                    )
                    // To prevent recursion
                    renderCapturedDetails = false
                    constructor.supertypes?.forEach(::render)
                    renderCapturedDetails = true
                    builder.append("]")
                }
            }

            is ConeClassLikeErrorLookupTag -> builder.append(renderDiagnostic(constructor.diagnostic, prefix = "ERROR CLASS: "))

            is ConeClassLikeLookupTag -> idRenderer.renderClassId(constructor.classId)
            is ConeClassifierLookupTag -> builder.append(constructor.name.asString())

            is ConeStubTypeConstructor -> builder.append("Stub (subtyping): ${constructor.variable}")
            is ConeIntegerLiteralType -> render(constructor)

            is ConeIntersectionType -> error(
                "`renderConstructor` mustn't be called with an intersection type argument. " +
                        "Call `render` to simply render the type or filter out intersection types on the call-site."
            )
        }
    }

    open fun renderDiagnostic(diagnostic: ConeDiagnostic, prefix: String = "", suffix: String = ""): String {
        return "$prefix${diagnostic.reason}$suffix"
    }

    private fun ConeClassLikeType.renderTypeArguments() {
        if (typeArguments.isEmpty()) return
        builder.append("<")
        for ((index, typeArgument) in typeArguments.withIndex()) {
            if (index > 0) {
                builder.append(", ")
            }
            typeArgument.render()
        }
        builder.append(">")
    }

    private fun ConeFlexibleType.renderForSameLookupTags(): Boolean {
        if (isTrivial && lowerBound !is ConeDefinitelyNotNullType) {
            render(lowerBound, nullabilityMarker = "!")
            return true
        }

        if (lowerBound is ConeLookupTagBasedType && upperBound is ConeLookupTagBasedType &&
            lowerBound.lookupTag == upperBound.lookupTag &&
            !lowerBound.isMarkedNullable && upperBound.isMarkedNullable
        ) {
            if (lowerBound !is ConeClassLikeType || lowerBound.typeArguments.isEmpty()) {
                if (upperBound !is ConeClassLikeType || upperBound.typeArguments.isEmpty()) {
                    render(lowerBound, nullabilityMarker = "!")
                    return true
                }
            }
        }
        return false
    }

    protected open fun render(flexibleType: ConeFlexibleType) {
        if (flexibleType.renderForSameLookupTags()) {
            return
        }
        builder.append("ft<")
        render(flexibleType.lowerBound)
        builder.append(", ")
        render(flexibleType.upperBound)
        builder.append(">")
    }

    protected open fun ConeKotlinType.renderAttributes() {
        if (!attributes.any()) return
        builder.append(attributeRenderer.render(attributes))
    }

    protected fun ConeKotlinType.renderNonCompilerAttributes() {
        val compilerAttributes = CompilerConeAttributes.classIdByCompilerAttributeKey
        attributes
            .filter { it.key !in compilerAttributes }
            .ifNotEmpty { builder.append(attributeRenderer.render(this)) }
    }

    protected fun ConeTypeProjection.render() {
        when (this) {
            ConeStarProjection -> {
                builder.append("*")
            }

            is ConeKotlinTypeConflictingProjection -> {
                builder.append("CONFLICTING-PROJECTION ")
                render(type)
            }

            is ConeKotlinTypeProjectionIn -> {
                builder.append("in ")
                render(type)
            }

            is ConeKotlinTypeProjectionOut -> {
                builder.append("out ")
                render(type)
            }

            is ConeKotlinType -> {
                render(this)
            }
        }
    }

    protected open fun render(type: ConeIntegerLiteralType) {
        when (type) {
            is ConeIntegerLiteralConstantType -> {
                builder.append("ILT: ${type.value}")
            }

            is ConeIntegerConstantOperatorType -> {
                builder.append("IOT")
            }
        }
    }

    protected open fun render(type: ConeDefinitelyNotNullType) {
        this.render(type.original)
        builder.append(" & Any")
    }

    protected open fun render(type: ConeIntersectionType) {
        builder.append("it(")
        for ((index, intersected) in type.intersectedTypes.withIndex()) {
            if (index > 0) {
                builder.append(" & ")
            }
            this.render(intersected)
        }
        builder.append(")")
    }
}

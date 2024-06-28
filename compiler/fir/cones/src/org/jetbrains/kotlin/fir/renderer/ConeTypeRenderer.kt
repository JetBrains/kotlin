/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

open class ConeTypeRenderer(
    private val attributeRenderer: ConeAttributeRenderer = ConeAttributeRenderer.ToString
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
        val typeArguments = type.typeArguments
        val isExtension = type.isExtensionFunctionType
        val (receiver, otherTypeArguments) = if (isExtension && typeArguments.size >= 2 && typeArguments.first() != ConeStarProjection) {
            typeArguments.first() to typeArguments.drop(1)
        } else {
            null to typeArguments.toList()
        }
        val arguments = otherTypeArguments.subList(0, otherTypeArguments.size - 1)
        val returnType = otherTypeArguments.last()
        if (receiver != null) {
            receiver.render()
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

    fun render(type: ConeKotlinType) {
        if (type !is ConeFlexibleType && type !is ConeDefinitelyNotNullType) {
            // We don't render attributes for flexible/definitely not null types here,
            // because bounds duplicate these attributes often
            type.renderAttributes()
        }
        when (type) {
            is ConeDefinitelyNotNullType -> {
                render(type.original)
                builder.append(" & Any")
            }

            is ConeIntersectionType -> {
                builder.append("it(")
                for ((index, intersected) in type.intersectedTypes.withIndex()) {
                    if (index > 0) {
                        builder.append(" & ")
                    }
                    render(intersected)
                }
                builder.append(")")
            }

            is ConeDynamicType -> {
                builder.append("dynamic")
            }

            is ConeFlexibleType -> {
                render(type)
            }

            is ConeErrorType -> builder.append("ERROR CLASS: ${type.diagnostic.reason}")

            is ConeSimpleKotlinType -> {
                renderConstructor(type.getConstructor())
                if (type is ConeClassLikeType) {
                    type.renderTypeArguments()
                }
            }
        }
        if (type !is ConeFlexibleType && type !is ConeErrorType) {
            builder.append(type.nullability.suffix)
        }
    }

    open fun renderConstructor(constructor: TypeConstructorMarker) {
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
            }

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
        if (lowerBound is ConeLookupTagBasedType && upperBound is ConeLookupTagBasedType &&
            lowerBound.lookupTag == upperBound.lookupTag &&
            lowerBound.nullability == ConeNullability.NOT_NULL && upperBound.nullability == ConeNullability.NULLABLE
        ) {
            if (lowerBound !is ConeClassLikeType || lowerBound.typeArguments.isEmpty()) {
                if (upperBound !is ConeClassLikeType || upperBound.typeArguments.isEmpty()) {
                    render(lowerBound)
                    builder.append("!")
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

    private fun ConeTypeProjection.render() {
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
}

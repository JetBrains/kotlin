/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.utils.evaluatedInitializer
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.unwrapOr
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

class ParameterNameTypeAttribute(
    val name: Name?,
    val annotations: List<FirAnnotation> = emptyList(),
) : ConeAttribute<ParameterNameTypeAttribute>() {
    init {
        require(annotations.isNotEmpty())
    }

    override fun union(other: ParameterNameTypeAttribute?): ParameterNameTypeAttribute? = null
    override fun intersect(other: ParameterNameTypeAttribute?): ParameterNameTypeAttribute? = null
    override fun add(other: ParameterNameTypeAttribute?): ParameterNameTypeAttribute = this

    override fun isSubtypeOf(other: ParameterNameTypeAttribute?): Boolean = true

    override val implementsEquality: Boolean get() = true
    override val key: KClass<out ParameterNameTypeAttribute> get() = KEY
    override val keepInInferredDeclarationType: Boolean get() = true

    override fun toString(): String = buildString {
        annotations.joinTo(this, separator = " ") { it.render() }
    }

    override fun renderForReadability(): String = buildString {
        annotations.joinTo(this, separator = " ") { FirRenderer.forReadability().renderElementAsString(it, trim = true) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParameterNameTypeAttribute

        return if (name != null) {
            name == other.name
        } else {
            annotations.first() == other.annotations.first()
        }
    }

    override fun hashCode(): Int {
        return name?.hashCode() ?: annotations.first().hashCode()
    }

    companion object {
        val KEY: KClass<out ParameterNameTypeAttribute> = ParameterNameTypeAttribute::class
        val ANNOTATION_CLASS_ID: ClassId get() = StandardNames.FqNames.parameterNameClassId
    }
}

val ConeAttributes.parameterNameAttribute: ParameterNameTypeAttribute? by ConeAttributes.attributeAccessor<ParameterNameTypeAttribute>()

/**
 * Access the [ParameterName] of the [ConeKotlinType].
 * This can be the name via lambda syntax or by explicit annotation.
 */
fun ConeKotlinType.parameterName(session: FirSession): Name? {
    val attribute = attributes.parameterNameAttribute ?: return null
    if (attribute.name != null) return attribute.name

    val annotation = attribute.annotations.first() // Attribute precondition.
    val name = annotation.getStringArgument(StandardNames.NAME, session) ?: return null
    return Name.identifier(name)
}

/**
 * Attempts to access the [ParameterName] annotation as if it has already been resolved.
 * This can be the name via lambda syntax or by explicit annotation.
 * Prefer to use the function overload which takes an [FirSession] whenever possible.
 */
val ConeKotlinType.parameterName: Name?
    get() {
        val attribute = attributes.parameterNameAttribute ?: return null
        if (attribute.name != null) return attribute.name

        val annotation = attribute.annotations.first() // Attribute precondition.
        val literal = when (val expression = annotation.argumentMapping.mapping[StandardNames.NAME]) {
            is FirLiteralExpression -> expression
            is FirPropertyAccessExpression -> expression.calleeReference.toResolvedPropertySymbol()
                ?.evaluatedInitializer?.unwrapOr<FirLiteralExpression> {}
            else -> null
        }
        val name = literal?.value as? String ?: return null
        return Name.identifier(name)
    }

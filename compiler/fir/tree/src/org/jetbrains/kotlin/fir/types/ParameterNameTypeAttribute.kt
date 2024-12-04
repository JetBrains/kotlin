/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import kotlin.reflect.KClass

class ParameterNameTypeAttribute(
    val annotation: FirAnnotation,
    val others: List<FirAnnotation> = emptyList(),
) : ConeAttribute<ParameterNameTypeAttribute>() {
    val name: Name by lazy {
        val expression = annotation.argumentMapping.mapping[StandardNames.NAME]
        val argument = (expression as? FirLiteralExpression)?.value as? String
        requireWithAttachment(argument != null, { "ParameterName argument not resolved to string." }) {
            withFirEntry("annotation", annotation)
        }
        Name.identifier(argument)
    }


    override fun union(other: ParameterNameTypeAttribute?): ParameterNameTypeAttribute? = null
    override fun intersect(other: ParameterNameTypeAttribute?): ParameterNameTypeAttribute? = null
    override fun add(other: ParameterNameTypeAttribute?): ParameterNameTypeAttribute = this

    override fun isSubtypeOf(other: ParameterNameTypeAttribute?): Boolean = true

    override val implementsEquality: Boolean get() = true
    override val key: KClass<out ParameterNameTypeAttribute> get() = KEY
    override val keepInInferredDeclarationType: Boolean get() = true

    override fun toString(): String = buildString {
        append(annotation.render())
        for (other in others) {
            append(" ")
            append(other.render())
        }
    }

    override fun renderForReadability(): String = buildString {
        append(FirRenderer.forReadability().renderElementAsString(annotation, trim = true))
        for (other in others) {
            append(" ")
            append(FirRenderer.forReadability().renderElementAsString(other, trim = true))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParameterNameTypeAttribute

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object {
        val KEY: KClass<out ParameterNameTypeAttribute> = ParameterNameTypeAttribute::class
        val ANNOTATION_CLASS_ID: ClassId get() = StandardNames.FqNames.parameterNameClassId
    }
}

val ConeAttributes.parameterNameAttribute: ParameterNameTypeAttribute? by ConeAttributes.attributeAccessor<ParameterNameTypeAttribute>()

val ConeKotlinType.parameterNameAnnotation: FirAnnotation? get() = attributes.parameterNameAttribute?.annotation

val ConeKotlinType.parameterName: Name? get() = attributes.parameterNameAttribute?.name

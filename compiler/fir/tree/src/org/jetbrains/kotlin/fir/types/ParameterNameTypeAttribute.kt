/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

class ParameterNameTypeAttribute(
    val name: Name?,
    val annotations: List<FirAnnotation>,
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

// Intentionally break binary compatibility but keep source compatibility to help compiler plugin migration.
@get:JvmName("parameterNameDeprecated")
@Deprecated(
    message = "Parameter name access without a session is not supported.",
    replaceWith = ReplaceWith("this.valueParameterName(session)"),
    level = DeprecationLevel.ERROR,
)
val ConeKotlinType.parameterName: Name?
    get() = throw UnsupportedOperationException("Parameter name access without a session is not supported.")

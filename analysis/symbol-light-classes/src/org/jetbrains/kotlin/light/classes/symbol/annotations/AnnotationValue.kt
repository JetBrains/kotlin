/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import java.util.Objects

/**
 * Represents a name-value annotation argument pair.
 */
internal class AnnotationArgument(val name: Name, val value: AnnotationValue)

internal fun KaNamedAnnotationValue.toLightClassAnnotationArgument(): AnnotationArgument {
    return AnnotationArgument(name, expression.toLightClassAnnotationValue())
}

/**
 * Represents an annotation applied to a declaration or a type.
 *
 * @param annotation The applied annotation value.
 * @param isDumb If `true`, the [annotation] only contains a [ClassId]. Both constructor pointer and arguments are not provided.
 * @param useSiteTarget Specifies a user-provided use-site annotation target if an annotation is applied on a declaration.
 * @param hasArguments `true` if the annotation has explicitly provided arguments.
 * @param index An index of the annotation in an owner, or `null` if the annotation provided as an annotation argument.
 */
internal data class AnnotationApplication(
    val annotation: AnnotationValue.Annotation,
    val isDumb: Boolean,
    val useSiteTarget: AnnotationUseSiteTarget?,
    val hasArguments: Boolean,
    val index: Int?,
)

internal fun KaAnnotation.toDumbLightClassAnnotationApplication(): AnnotationApplication {
    val value = AnnotationValue.Annotation(
        classId,
        constructorSymbolPointer = constructorSymbol?.createPointer(),
        arguments = emptyList(),
        sourcePsi = null
    )

    return AnnotationApplication(value, true, useSiteTarget, hasArguments, index)
}

internal fun KaAnnotation.toLightClassAnnotationApplication(): AnnotationApplication {
    return AnnotationApplication(toLightClassAnnotationValue(), false, useSiteTarget, hasArguments, index)
}

internal sealed class AnnotationValue {
    abstract val sourcePsi: KtElement?

    /**
     * Represents an unsupported expression passed as an annotation value.
     */
    class Unsupported(override val sourcePsi: KtElement?) : AnnotationValue() {
        override fun equals(other: Any?) = other === this || other is Unsupported
        override fun hashCode() = 0
        override fun toString() = "Unsupported"
    }

    /**
     * Represents an array literal (both in the literal syntax (`[1, 2]`) and in the `intArrayOf(1, 2)` form.
     */
    class Array(val values: List<AnnotationValue>, override val sourcePsi: KtElement?) : AnnotationValue() {
        override fun equals(other: Any?) = this === other || (other is Array && values == other.values)
        override fun hashCode() = values.hashCode()
        override fun toString() = "Array(" + values.joinToString() + ")"
    }

    /**
     * Represents an annotation passed as an annotation value.
     */
    class Annotation(
        val classId: ClassId?,
        val constructorSymbolPointer: KaSymbolPointer<KaConstructorSymbol>?,
        val arguments: List<AnnotationArgument>,
        override val sourcePsi: KtCallElement?
    ) : AnnotationValue() {
        override fun equals(other: Any?): Boolean {
            return this === other || (other is Annotation && classId == other.classId && arguments == other.arguments)
        }

        override fun hashCode() = Objects.hash(classId, arguments)
        override fun toString() = "Annotation(classId=$classId, arguments=$arguments)"
    }

    /**
     * Represents a [KClass] class reference (`Foo::class`).
     *
     * @param classId A [ClassId] for a reference to a non-local class.
     * @param isError `true` if the reference points to an unresolved class.
     */
    class KClass(val classId: ClassId?, val isError: Boolean, override val sourcePsi: KtElement?) : AnnotationValue() {
        override fun equals(other: Any?): Boolean {
            return this === other || (other is KClass && classId == other.classId && isError == other.isError)
        }

        override fun hashCode() = Objects.hash(classId, isError)
        override fun toString() = "KClass(classid=$classId, isError=$isError)"
    }

    /**
     * Represents a enumeration value.
     */
    class EnumValue(val callableId: CallableId?, override val sourcePsi: KtElement?) : AnnotationValue() {
        override fun equals(other: Any?) = this === other || (other is EnumValue && other.callableId == callableId)
        override fun hashCode() = callableId.hashCode()
        override fun toString() = "EnumValue($callableId)"
    }

    /**
     * Represents a primitive or a [String] constant value.
     */
    class Constant(val constant: KaConstantValue, override val sourcePsi: KtElement?) : AnnotationValue() {
        override fun equals(other: Any?) = this === other || (other is Constant && constant == other.constant)
        override fun hashCode(): Int = constant.hashCode()
        override fun toString() = "Constant(" + constant.render() + ")"
    }
}

internal fun KaAnnotationValue.toLightClassAnnotationValue(): AnnotationValue {
    return when (this) {
        is KaAnnotationValue.UnsupportedValue -> AnnotationValue.Unsupported(sourcePsi)
        is KaAnnotationValue.ArrayValue -> AnnotationValue.Array(values.map { it.toLightClassAnnotationValue() }, sourcePsi)
        is KaAnnotationValue.NestedAnnotationValue -> annotation.toLightClassAnnotationValue()
        is KaAnnotationValue.ClassLiteralValue -> toLightClassAnnotationValue()
        is KaAnnotationValue.EnumEntryValue -> AnnotationValue.EnumValue(callableId, sourcePsi)
        is KaAnnotationValue.ConstantValue -> AnnotationValue.Constant(value, sourcePsi)
    }
}

internal fun KaAnnotationValue.ClassLiteralValue.toLightClassAnnotationValue(): AnnotationValue.KClass {
    when (val type = type) {
        is KaClassType -> {
            val classId = type.classId.takeUnless { it.isLocal }
            return AnnotationValue.KClass(classId, isError = false, sourcePsi)
        }

        else -> {
            val classId = classId?.takeUnless { it.isLocal }
            return AnnotationValue.KClass(classId, isError = true, sourcePsi)
        }
    }
}

internal fun KaAnnotation.toLightClassAnnotationValue(): AnnotationValue.Annotation {
    val arguments = arguments.map { AnnotationArgument(it.name, it.expression.toLightClassAnnotationValue()) }
    return AnnotationValue.Annotation(classId, constructorSymbol?.createPointer(), arguments, psi)
}
/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import java.util.*

/**
 * Represents a name-value annotation argument pair.
 */
internal class AnnotationArgument(val name: Name, val value: AnnotationValue)

internal fun KaNamedAnnotationValue.toLightClassAnnotationArgument(useSiteModule: KaModule): AnnotationArgument {
    return AnnotationArgument(name, expression.toLightClassAnnotationValue(useSiteModule))
}

/**
 * Represents an annotation applied to a declaration or a type.
 *
 * @param annotation The applied annotation value.
 * @param isDumb If `true`, the [annotation] only contains a [ClassId]. Both constructor pointer and arguments are not provided.
 * @param relativeIndex A relative index of the annotation with the same [ClassId] in an owner.
 */
internal data class AnnotationApplication(
    val annotation: AnnotationValue.Annotation,
    val isDumb: Boolean,
    val relativeIndex: Int,
)

/**
 * @return `null` if the annotation is unresolved, as such an annotation has no [ClassId] to be represented with in a light class.
 */
internal fun KaAnnotation.toDumbLightClassAnnotationApplication(relativeIndex: Int, kaModule: KaModule): AnnotationApplication? {
    val value = AnnotationValue.Annotation(
        classId = classId ?: return null,
        useSiteModule = kaModule,
        constructorSymbolPointer = constructorSymbol?.createPointer(),
        arguments = emptyList(),
        sourcePsi = psi,
    )

    return AnnotationApplication(value, true, relativeIndex)
}

/**
 * @return `null` if the annotation is unresolved, as such an annotation has no [ClassId] to be represented with in a light class.
 */
internal fun KaAnnotation.toLightClassAnnotationApplication(relativeIndex: Int, kaModule: KaModule): AnnotationApplication? {
    val value = toLightClassAnnotationValue(kaModule) ?: return null
    return AnnotationApplication(value, false, relativeIndex)
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
        val classId: ClassId,
        val useSiteModule: KaModule,
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

internal fun KaAnnotationValue.toLightClassAnnotationValue(useSiteModule: KaModule): AnnotationValue {
    return when (this) {
        is KaAnnotationValue.UnsupportedValue -> AnnotationValue.Unsupported(sourcePsi)
        is KaAnnotationValue.ArrayValue -> AnnotationValue.Array(values.map { it.toLightClassAnnotationValue(useSiteModule) }, sourcePsi)
        // An unresolved nested annotation has no ClassId, so it cannot be represented as a light class annotation
        is KaAnnotationValue.NestedAnnotationValue ->
            annotation.toLightClassAnnotationValue(useSiteModule) ?: AnnotationValue.Unsupported(sourcePsi)
        is KaAnnotationValue.ClassLiteralValue -> toLightClassAnnotationValue()
        is KaAnnotationValue.EnumEntryValue -> AnnotationValue.EnumValue(callableId, sourcePsi)
        is KaAnnotationValue.ConstantValue -> AnnotationValue.Constant(value, sourcePsi)
    }
}

@OptIn(ClassIdBasedLocality::class)
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

/**
 * @return `null` if the annotation is unresolved, as such an annotation has no [ClassId] to be represented with in a light class.
 */
internal fun KaAnnotation.toLightClassAnnotationValue(useSiteModule: KaModule): AnnotationValue.Annotation? {
    val classId = classId ?: return null
    val arguments = arguments.map { AnnotationArgument(it.name, it.expression.toLightClassAnnotationValue(useSiteModule)) }
    return AnnotationValue.Annotation(classId, useSiteModule, constructorSymbol?.createPointer(), arguments, psi)
}

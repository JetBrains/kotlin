/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.load.java.AbstractAnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import org.jetbrains.kotlin.load.java.JavaTypeQualifiersByElementType
import org.jetbrains.kotlin.load.java.typeEnhancement.*
import org.jetbrains.kotlin.load.java.typeEnhancement.MutabilityQualifier.MUTABLE
import org.jetbrains.kotlin.load.java.typeEnhancement.MutabilityQualifier.READ_ONLY
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier.FORCE_FLEXIBILITY
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier.NOT_NULL
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.jvm.internal.types.*

internal class ReflectSignatureParts(
    override val containerApplicabilityType: AnnotationQualifierApplicabilityType,
    override val containerIsVarargParameter: Boolean = false,
) : AbstractSignatureParts<Annotation>() {
    override val annotationTypeQualifierResolver: AbstractAnnotationTypeQualifierResolver<Annotation>
        get() = ReflectAnnotationTypeQualifierResolver
    override val enableImprovementsInStrictMode: Boolean
        get() = true
    override val containerAnnotations: Iterable<Annotation>
        get() = emptyList() // We don't support JSR-305 annotations in kotlin-reflect for now.
    override val containerDefaultTypeQualifiers: JavaTypeQualifiersByElementType?
        get() = null
    override val isCovariant: Boolean
        get() = containerApplicabilityType == AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE
    override val skipRawTypeArguments: Boolean
        get() = false
    override val typeSystem: TypeSystemContext
        get() = ReflectTypeSystemContext
    override val isK2: Boolean
        get() = true

    override fun Annotation.forceWarning(unenhancedType: KotlinTypeMarker?): Boolean = false

    override val KotlinTypeMarker.annotations: Iterable<Annotation>
        get() = (this as AbstractKType).annotations
    override val KotlinTypeMarker.enhancedForWarnings: KotlinTypeMarker?
        get() = null
    override val KotlinTypeMarker.fqNameUnsafe: FqNameUnsafe?
        get() = (this as AbstractKType).mutableCollectionClass?.qualifiedName?.let(::FqNameUnsafe)
            ?: (classifier as? KClassImpl<*>)?.classId?.asSingleFqName()?.toUnsafe()

    override fun KotlinTypeMarker.isEqual(other: KotlinTypeMarker): Boolean =
        areEqualKTypes(this as KType, other as KType)

    override fun KotlinTypeMarker.isArrayOrPrimitiveArray(): Boolean {
        val classifier = (this as? AbstractKType)?.classifier as? KClass<*> ?: return false
        return classifier.java.isArray
    }

    override val TypeParameterMarker.isFromJava: Boolean
        get() = (this as? KTypeParameterImpl)?.isFromJava() == true
}

private object ReflectAnnotationTypeQualifierResolver : AbstractAnnotationTypeQualifierResolver<Annotation>(
    JavaTypeEnhancementState.getDefault(KotlinVersion.CURRENT),
) {
    override val Annotation.metaAnnotations: Iterable<Annotation>
        get() = emptyList()
    override val Annotation.key: Any
        get() = annotationClass
    override val Annotation.fqName: FqName?
        get() = annotationClass.qualifiedName?.let { FqName(it) }

    override fun Annotation.enumArguments(onlyValue: Boolean): Iterable<String> =
        throw KotlinReflectionInternalError("Should not be called because kotlin-reflect doesn't support JSR-305 annotations.")

    override val isK2: Boolean
        get() = true
}

private fun KTypeParameterImpl.isFromJava(): Boolean {
    val containingClass = when (val container = container) {
        is KClassImpl<*> -> container
        is ReflectKCallable<*> -> container.container as? KClassImpl<*>
        else -> null
    }
    return containingClass != null && containingClass.kmClass == null
}

internal fun AbstractKType.enhance(qualifiers: IndexedJavaTypeQualifiers, isSuperTypesEnhancement: Boolean = false): AbstractKType {
    return enhancePossiblyFlexible(qualifiers, 0, isSuperTypesEnhancement).type ?: this
}

private class EnhancementResult(val type: AbstractKType?, val subtreeSize: Int)

private fun AbstractKType.enhancePossiblyFlexible(
    qualifiers: IndexedJavaTypeQualifiers, index: Int, isSuperTypesEnhancement: Boolean,
): EnhancementResult {
    val lowerBound = lowerBoundIfFlexible()
    val upperBound = upperBoundIfFlexible()
    if (lowerBound != null && upperBound != null) {
        val lowerResult =
            lowerBound.enhanceInflexible(qualifiers, index, TypeComponentPosition.FLEXIBLE_LOWER, isRawType, isSuperTypesEnhancement)
        val upperResult =
            upperBound.enhanceInflexible(qualifiers, index, TypeComponentPosition.FLEXIBLE_UPPER, isRawType, isSuperTypesEnhancement)
        assert(lowerResult.subtreeSize == upperResult.subtreeSize) {
            "Different tree sizes of bounds: " +
                    "lower = ($lowerBound, ${lowerResult.subtreeSize}), " +
                    "upper = ($upperBound, ${upperResult.subtreeSize})"
        }
        val type = when {
            lowerResult.type == null && upperResult.type == null -> null
            else -> FlexibleKType.create(lowerResult.type ?: lowerBound, upperResult.type ?: upperBound, isRawType)
        }
        return EnhancementResult(type, lowerResult.subtreeSize)
    }

    return enhanceInflexible(qualifiers, index, TypeComponentPosition.INFLEXIBLE, isBoundOfRawType = false, isSuperTypesEnhancement)
}

private fun AbstractKType.enhanceInflexible(
    qualifiers: IndexedJavaTypeQualifiers, index: Int, position: TypeComponentPosition, isBoundOfRawType: Boolean,
    isSuperTypesEnhancement: Boolean,
): EnhancementResult {
    val shouldEnhance = position.shouldEnhance()
    val shouldEnhanceArguments = !isSuperTypesEnhancement || !isBoundOfRawType
    if (!shouldEnhance && arguments.isEmpty()) return EnhancementResult(null, 1)

    val originalClass = mutableCollectionClass ?: classifier ?: return EnhancementResult(null, 1)

    val effectiveQualifiers = qualifiers(index)
    val enhancedClassifier = originalClass.enhanceMutability(effectiveQualifiers, position)
    val enhancedNullability = getEnhancedNullability(effectiveQualifiers, position)

    val typeConstructor = enhancedClassifier ?: originalClass
    var globalArgIndex = index + 1
    val enhancedArguments = arguments.map { arg ->
        val enhanced = when {
            !shouldEnhanceArguments -> EnhancementResult(null, 0)
            arg != KTypeProjection.STAR ->
                (arg.type as AbstractKType).enhancePossiblyFlexible(qualifiers, globalArgIndex, isSuperTypesEnhancement)
            qualifiers(globalArgIndex).nullability == FORCE_FLEXIBILITY ->
                (arg.type as AbstractKType).let {
                    // Given `C<T extends @Nullable V>`, unannotated `C<?>` is `C<out (V..V?)>`.
                    EnhancementResult(
                        FlexibleKType.create(
                            (it.lowerBoundIfFlexible() ?: this).makeNullableAsSpecified(false),
                            (it.upperBoundIfFlexible() ?: this).makeNullableAsSpecified(true),
                            isRawType = false,
                        ), 1
                    )
                }
            else -> EnhancementResult(null, 1)
        }
        globalArgIndex += enhanced.subtreeSize
        when {
            enhanced.type != null -> KTypeProjection(arg.variance, enhanced.type)
            enhancedClassifier != null && arg != KTypeProjection.STAR -> KTypeProjection(arg.variance, arg.type)
            enhancedClassifier != null -> KTypeProjection.STAR
            else -> null
        }
    }

    val subtreeSize = globalArgIndex - index
    if (enhancedClassifier == null && enhancedNullability == null && enhancedArguments.all { it == null })
        return EnhancementResult(null, subtreeSize)

    val enhancedType = SimpleKType(
        typeConstructor,
        enhancedArguments.zip(arguments) { enhanced, original -> enhanced ?: original },
        enhancedNullability ?: isMarkedNullable,
        lazyAnnotations, // In contrast to the compiler, we're not adding synthetic EnhancedNullability/EnhancedMutability annotations.
        abbreviation,
        isDefinitelyNotNullType || effectiveQualifiers.definitelyNotNull,
        isNothingType,
        isSuspendFunctionType,
        enhancedClassifier as? MutableCollectionKClass<*>,
    )

    return EnhancementResult(enhancedType, subtreeSize)
}

private fun KClassifier.enhanceMutability(
    qualifiers: JavaTypeQualifiers,
    position: TypeComponentPosition,
): KClassifier? = when {
    !position.shouldEnhance() -> null
    this !is KClass<*> -> null
    qualifiers.mutability == READ_ONLY && position == TypeComponentPosition.FLEXIBLE_LOWER && this is MutableCollectionKClass<*> ->
        readonlyClass
    qualifiers.mutability == MUTABLE && position == TypeComponentPosition.FLEXIBLE_UPPER && this !is MutableCollectionKClass<*> ->
        getMutableCollectionKClass(this)
    else -> null
}

private fun getEnhancedNullability(qualifiers: JavaTypeQualifiers, position: TypeComponentPosition): Boolean? {
    if (!position.shouldEnhance()) return null
    return when (qualifiers.nullability) {
        NullabilityQualifier.NULLABLE -> true
        NOT_NULL -> false
        else -> null
    }
}

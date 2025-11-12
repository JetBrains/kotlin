/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.runtime.structure.primitiveByWrapper
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.jvm.internal.*
import kotlin.reflect.jvm.jvmErasure

internal class DescriptorKType(
    val type: KotlinType,
    computeJavaType: (() -> Type)?,
    private val isAbbreviation: Boolean,
) : AbstractKType(computeJavaType) {
    constructor(type: KotlinType, computeJavaType: (() -> Type)? = null) : this(type, computeJavaType, isAbbreviation = false)

    override val classifier: KClassifier? by ReflectProperties.lazySoft { convert(type) }

    private fun convert(type: KotlinType): KClassifier? {
        if (isAbbreviation) {
            // Package scope in kotlin-reflect cannot load type aliases because it requires to know the file class where the typealias
            // is declared. Descriptor deserialization creates a "not found" MockClassDescriptor in this case.
            (type.constructor.declarationDescriptor as? NotFoundClasses.MockClassDescriptor)?.let { notFoundClass ->
                return KTypeAliasImpl(notFoundClass.fqNameSafe)
            }
        }
        when (val descriptor = type.constructor.declarationDescriptor) {
            is ClassDescriptor -> {
                val jClass = descriptor.toJavaClass() ?: return null
                if (KotlinBuiltIns.isArray(type)) {
                    val argument = type.arguments.singleOrNull()?.type ?: return KClassImpl(jClass)
                    // Make the array element type nullable to make sure that `kotlin.Array<Int>` is mapped to `[Ljava/lang/Integer;`
                    // instead of `[I`.
                    val elementClassifier =
                        convert(argument.makeNullable())
                            ?: throw KotlinReflectionInternalError("Cannot determine classifier for array element type: $this")
                    return KClassImpl(elementClassifier.jvmErasure.java.createArrayType())
                }

                if (!TypeUtils.isNullableType(type)) {
                    return KClassImpl(jClass.primitiveByWrapper ?: jClass)
                }

                return KClassImpl(jClass)
            }
            is TypeParameterDescriptor -> return KTypeParameterImpl(descriptor.toContainer(), descriptor)
            else -> return null
        }
    }

    override val arguments: List<KTypeProjection> by ReflectProperties.lazySoft arguments@{
        val typeArguments = type.arguments
        if (typeArguments.isEmpty()) return@arguments emptyList()

        typeArguments.mapIndexed { i, typeProjection ->
            typeProjection.toKTypeProjection(if (computeJavaType == null) null else convertTypeArgumentToJavaType({ this }, i))
        }
    }

    private fun TypeProjection.toKTypeProjection(computeJavaType: (() -> Type)? = null): KTypeProjection {
        if (isStarProjection) return KTypeProjection.STAR

        val result = DescriptorKType(type, computeJavaType)
        return when (projectionKind) {
            Variance.INVARIANT -> KTypeProjection.invariant(result)
            Variance.IN_VARIANCE -> KTypeProjection.contravariant(result)
            Variance.OUT_VARIANCE -> KTypeProjection.covariant(result)
        }
    }

    override val isMarkedNullable: Boolean
        get() = type.isMarkedNullable

    override val annotations: List<Annotation>
        get() = type.computeAnnotations()

    override fun makeNullableAsSpecified(nullable: Boolean): AbstractKType {
        // If the type is not marked nullable, it's either a non-null type or a platform type.
        if (!type.isFlexible() && isMarkedNullable == nullable) return this

        return DescriptorKType(TypeUtils.makeNullableAsSpecified(type, nullable))
    }

    override fun makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull: Boolean): AbstractKType {
        val result =
            if (isDefinitelyNotNull)
                DefinitelyNotNullType.makeDefinitelyNotNull(type.unwrap(), true) ?: return this
            else
                (type as? DefinitelyNotNullType)?.original ?: return this
        return DescriptorKType(result)
    }

    override val abbreviation: KType?
        get() = type.getAbbreviation()?.let { DescriptorKType(it, computeJavaType, isAbbreviation = true) }

    override val isDefinitelyNotNullType: Boolean
        get() = type.isDefinitelyNotNullType

    override val isNothingType: Boolean
        get() = KotlinBuiltIns.isNothingOrNullableNothing(type)

    override val mutableCollectionClass: KClass<*>?
        get() {
            val classDescriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
            if (!JavaToKotlinClassMapper.isMutable(classDescriptor)) return null
            if (useK1Implementation) {
                return MutableCollectionKClass(
                    classifier as KClass<*>,
                    classDescriptor.fqNameSafe.asString(),
                    { container ->
                        classDescriptor.declaredTypeParameters.map { descriptor -> KTypeParameterImpl(container, descriptor) }
                    },
                    {
                        classDescriptor.typeConstructor.supertypes.map(::DescriptorKType)
                    },
                )
            }
            return getMutableCollectionKClass(classDescriptor.fqNameSafe, classifier as KClass<*>)
        }

    override val isSuspendFunctionType: Boolean
        get() = type.isSuspendFunctionType

    override val isRawType: Boolean
        get() = type is RawType

    override fun lowerBoundIfFlexible(): AbstractKType? =
        when (val unwrapped = type.unwrap()) {
            is FlexibleType -> DescriptorKType(unwrapped.lowerBound)
            else -> null
        }

    override fun upperBoundIfFlexible(): AbstractKType? =
        when (val unwrapped = type.unwrap()) {
            is FlexibleType -> DescriptorKType(unwrapped.upperBound)
            else -> null
        }

    override fun equals(other: Any?): Boolean =
        if (useK1Implementation) {
            other is DescriptorKType && type == other.type && classifier == other.classifier && arguments == other.arguments
        } else super.equals(other)

    override fun hashCode(): Int =
        if (useK1Implementation) {
            (31 * ((31 * type.hashCode()) + classifier.hashCode())) + arguments.hashCode()
        } else super.hashCode()
}

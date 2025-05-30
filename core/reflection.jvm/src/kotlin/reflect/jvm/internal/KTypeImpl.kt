/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.runtime.structure.parameterizedTypeArguments
import org.jetbrains.kotlin.descriptors.runtime.structure.primitiveByWrapper
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.jvm.jvmErasure

internal class KTypeImpl(
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
            is TypeParameterDescriptor -> return KTypeParameterImpl(null, descriptor)
            else -> return null
        }
    }

    override val arguments: List<KTypeProjection> by ReflectProperties.lazySoft arguments@{
        val typeArguments = type.arguments
        if (typeArguments.isEmpty()) return@arguments emptyList<KTypeProjection>()

        val parameterizedTypeArguments by lazy(PUBLICATION) { javaType!!.parameterizedTypeArguments }

        typeArguments.mapIndexed { i, typeProjection ->
            if (typeProjection.isStarProjection) {
                KTypeProjection.STAR
            } else {
                val type = KTypeImpl(typeProjection.type, if (computeJavaType == null) null else fun(): Type {
                    return when (val javaType = javaType) {
                        is Class<*> -> {
                            // It's either an array or a raw type.
                            // TODO: return upper bound of the corresponding parameter for a raw type?
                            if (javaType.isArray) javaType.componentType else Any::class.java
                        }
                        is GenericArrayType -> {
                            if (i != 0) throw KotlinReflectionInternalError("Array type has been queried for a non-0th argument: $this")
                            javaType.genericComponentType
                        }
                        is ParameterizedType -> {
                            val argument = parameterizedTypeArguments[i]
                            // In "Foo<out Bar>", the JVM type of the first type argument should be "Bar", not "? extends Bar"
                            if (argument !is WildcardType) argument
                            else argument.lowerBounds.firstOrNull() ?: argument.upperBounds.first()
                        }
                        else -> throw KotlinReflectionInternalError("Non-generic type has been queried for arguments: $this")
                    }
                })
                when (typeProjection.projectionKind) {
                    Variance.INVARIANT -> KTypeProjection.invariant(type)
                    Variance.IN_VARIANCE -> KTypeProjection.contravariant(type)
                    Variance.OUT_VARIANCE -> KTypeProjection.covariant(type)
                }
            }
        }
    }

    override val isMarkedNullable: Boolean
        get() = type.isMarkedNullable

    override val annotations: List<Annotation>
        get() = type.computeAnnotations()

    override fun isSubtypeOf(other: AbstractKType): Boolean {
        return type.isSubtypeOf((other as KTypeImpl).type)
    }

    override fun makeNullableAsSpecified(nullable: Boolean): AbstractKType {
        // If the type is not marked nullable, it's either a non-null type or a platform type.
        if (!type.isFlexible() && isMarkedNullable == nullable) return this

        return KTypeImpl(TypeUtils.makeNullableAsSpecified(type, nullable), computeJavaType)
    }

    override fun makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull: Boolean): AbstractKType {
        val result =
            if (isDefinitelyNotNull)
                DefinitelyNotNullType.makeDefinitelyNotNull(type.unwrap(), true) ?: type
            else
                (type as? DefinitelyNotNullType)?.original ?: type
        return KTypeImpl(result, computeJavaType)
    }

    override val abbreviation: KType?
        get() = type.getAbbreviation()?.let { KTypeImpl(it, computeJavaType, isAbbreviation = true) }

    override val isDefinitelyNotNullType: Boolean
        get() = type.isDefinitelyNotNullType

    override val isNothingType: Boolean
        get() = KotlinBuiltIns.isNothingOrNullableNothing(type)

    override val isMutableCollectionType: Boolean
        get() = (type.constructor.declarationDescriptor as? ClassDescriptor)?.let(JavaToKotlinClassMapper::isMutable) == true

    override val isSuspendFunctionType: Boolean
        get() = type.isSuspendFunctionType

    override val isRawType: Boolean
        get() = type is RawType

    override fun lowerBoundIfFlexible(): AbstractKType? =
        when (val unwrapped = type.unwrap()) {
            is FlexibleType -> KTypeImpl(unwrapped.lowerBound)
            else -> null
        }

    override fun upperBoundIfFlexible(): AbstractKType? =
        when (val unwrapped = type.unwrap()) {
            is FlexibleType -> KTypeImpl(unwrapped.upperBound)
            else -> null
        }

    override fun equals(other: Any?) =
        other is KTypeImpl && type == other.type && classifier == other.classifier && arguments == other.arguments

    override fun hashCode() =
        (31 * ((31 * type.hashCode()) + classifier.hashCode())) + arguments.hashCode()
}

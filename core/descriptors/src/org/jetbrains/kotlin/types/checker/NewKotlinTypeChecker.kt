/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.calls.inference.CapturedTypeConstructorImpl
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.AbstractNullabilityChecker.hasNotNullSupertype
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext.SupertypesPolicy
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.typeUtil.makeNullable

object SimpleClassicTypeSystemContext : ClassicTypeSystemContext

class ClassicTypeSystemContextImpl(override val builtIns: KotlinBuiltIns) : ClassicTypeSystemContext

object StrictEqualityTypeChecker {

    /**
     * String! != String & A<String!> != A<String>, also A<in Nothing> != A<out Any?>
     * also A<*> != A<out Any?>
     * different error types non-equals even errorTypeEqualToAnything
     */
    fun strictEqualTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractStrictEqualityTypeChecker.strictEqualTypes(SimpleClassicTypeSystemContext, a, b)
    }

    fun strictEqualTypes(a: SimpleType, b: SimpleType): Boolean {
        return AbstractStrictEqualityTypeChecker.strictEqualTypes(SimpleClassicTypeSystemContext, a, b)
    }

}

object ErrorTypesAreEqualToAnything : KotlinTypeChecker {
    override fun isSubtypeOf(subtype: KotlinType, supertype: KotlinType): Boolean =
        NewKotlinTypeChecker.Default.run { ClassicTypeCheckerContext(true).isSubtypeOf(subtype.unwrap(), supertype.unwrap()) }

    override fun equalTypes(a: KotlinType, b: KotlinType): Boolean =
        NewKotlinTypeChecker.Default.run { ClassicTypeCheckerContext(true).equalTypes(a.unwrap(), b.unwrap()) }
}

interface NewKotlinTypeChecker : KotlinTypeChecker {
    val kotlinTypeRefiner: KotlinTypeRefiner
    val overridingUtil: OverridingUtil

    fun transformToNewType(type: UnwrappedType): UnwrappedType

    companion object {
        val Default = NewKotlinTypeCheckerImpl(KotlinTypeRefiner.Default)
    }
}


class NewKotlinTypeCheckerImpl(override val kotlinTypeRefiner: KotlinTypeRefiner) : NewKotlinTypeChecker {
    override val overridingUtil: OverridingUtil = OverridingUtil.createWithTypeRefiner(kotlinTypeRefiner)

    override fun isSubtypeOf(subtype: KotlinType, supertype: KotlinType): Boolean =
        ClassicTypeCheckerContext(true, kotlinTypeRefiner = kotlinTypeRefiner)
            .isSubtypeOf(subtype.unwrap(), supertype.unwrap()) // todo fix flag errorTypeEqualsToAnything

    override fun equalTypes(a: KotlinType, b: KotlinType): Boolean =
        ClassicTypeCheckerContext(false, kotlinTypeRefiner = kotlinTypeRefiner).equalTypes(a.unwrap(), b.unwrap())

    fun ClassicTypeCheckerContext.equalTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractTypeChecker.equalTypes(this as AbstractTypeCheckerContext, a, b)
    }

    fun ClassicTypeCheckerContext.isSubtypeOf(subType: UnwrappedType, superType: UnwrappedType): Boolean {
        return AbstractTypeChecker.isSubtypeOf(this as AbstractTypeCheckerContext, subType, superType)
    }

    fun transformToNewType(type: SimpleType): SimpleType {
        val constructor = type.constructor
        when (constructor) {
            // Type itself can be just SimpleTypeImpl, not CapturedType. see KT-16147
            is CapturedTypeConstructorImpl -> {
                val lowerType = constructor.projection.takeIf { it.projectionKind == Variance.IN_VARIANCE }?.type?.unwrap()

                // it is incorrect calculate this type directly because of recursive star projections
                if (constructor.newTypeConstructor == null) {
                    constructor.newTypeConstructor =
                        NewCapturedTypeConstructor(constructor.projection, constructor.supertypes.map { it.unwrap() })
                }
                return NewCapturedType(
                    CaptureStatus.FOR_SUBTYPING, constructor.newTypeConstructor!!,
                    lowerType, type.annotations, type.isMarkedNullable
                )
            }

            is IntegerValueTypeConstructor -> {
                val newConstructor =
                    IntersectionTypeConstructor(constructor.supertypes.map { TypeUtils.makeNullableAsSpecified(it, type.isMarkedNullable) })
                return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
                    type.annotations,
                    newConstructor,
                    listOf(),
                    false,
                    type.memberScope
                )
            }

            is IntersectionTypeConstructor -> if (type.isMarkedNullable) {
                val newConstructor = constructor.transformComponents(transform = { it.makeNullable() }) ?: constructor
                return newConstructor.createType()

            }
        }

        return type
    }

    override fun transformToNewType(type: UnwrappedType): UnwrappedType =
        when (type) {
            is SimpleType -> transformToNewType(type)
            is FlexibleType -> {
                val newLower = transformToNewType(type.lowerBound)
                val newUpper = transformToNewType(type.upperBound)
                if (newLower !== type.lowerBound || newUpper !== type.upperBound) {
                    KotlinTypeFactory.flexibleType(newLower, newUpper)
                } else {
                    type
                }
            }
        }.inheritEnhancement(type)
}

object NullabilityChecker {

    fun isSubtypeOfAny(type: UnwrappedType): Boolean =
        SimpleClassicTypeSystemContext
            .newBaseTypeCheckerContext(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
            .hasNotNullSupertype(type.lowerIfFlexible(), SupertypesPolicy.LowerIfFlexible)
}

fun UnwrappedType.hasSupertypeWithGivenTypeConstructor(typeConstructor: TypeConstructor) =
    ClassicTypeCheckerContext(false).anySupertype(lowerIfFlexible(), {
        require(it is SimpleType)
        it.constructor == typeConstructor
    }, { SupertypesPolicy.LowerIfFlexible })

fun UnwrappedType.anySuperTypeConstructor(predicate: (TypeConstructor) -> Boolean) =
    ClassicTypeCheckerContext(false).anySupertype(lowerIfFlexible(), {
        require(it is SimpleType)
        predicate(it.constructor)
    }, { SupertypesPolicy.LowerIfFlexible })

/**
 * ClassType means that type constructor for this type is type for real class or interface
 */
val SimpleType.isClassType: Boolean get() = constructor.declarationDescriptor is ClassDescriptor

/**
 * SingleClassifierType is one of the following types:
 *  - classType
 *  - type for type parameter
 *  - captured type
 *
 * Such types can contains error types in our arguments, but type constructor isn't errorTypeConstructor
 */
val SimpleType.isSingleClassifierType: Boolean
    get() = !isError &&
            constructor.declarationDescriptor !is TypeAliasDescriptor &&
            (constructor.declarationDescriptor != null || this is CapturedType || this is NewCapturedType || this is DefinitelyNotNullType)

val SimpleType.isIntersectionType: Boolean
    get() = constructor is IntersectionTypeConstructor

val SimpleType.isIntegerLiteralType: Boolean
    get() = constructor is IntegerLiteralTypeConstructor

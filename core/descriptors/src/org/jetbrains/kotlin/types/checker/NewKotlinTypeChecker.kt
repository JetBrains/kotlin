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
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.AbstractNullabilityChecker.hasNotNullSupertype
import org.jetbrains.kotlin.types.TypeCheckerState.SupertypesPolicy

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
        NewKotlinTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).isSubtypeOf(subtype.unwrap(), supertype.unwrap())
        }

    override fun equalTypes(a: KotlinType, b: KotlinType): Boolean =
        NewKotlinTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).equalTypes(a.unwrap(), b.unwrap())
        }
}

interface NewKotlinTypeChecker : KotlinTypeChecker {
    val kotlinTypeRefiner: KotlinTypeRefiner
    val kotlinTypePreparator: KotlinTypePreparator
    val overridingUtil: OverridingUtil

    companion object {
        val Default = NewKotlinTypeCheckerImpl(KotlinTypeRefiner.Default)
    }
}


class NewKotlinTypeCheckerImpl(
    override val kotlinTypeRefiner: KotlinTypeRefiner,
    override val kotlinTypePreparator: KotlinTypePreparator = KotlinTypePreparator.Default
) : NewKotlinTypeChecker {
    override val overridingUtil: OverridingUtil = OverridingUtil.createWithTypeRefiner(kotlinTypeRefiner)

    override fun isSubtypeOf(subtype: KotlinType, supertype: KotlinType): Boolean =
        createClassicTypeCheckerState(
            true, kotlinTypeRefiner = kotlinTypeRefiner, kotlinTypePreparator = kotlinTypePreparator
        ).isSubtypeOf(subtype.unwrap(), supertype.unwrap()) // todo fix flag errorTypeEqualsToAnything

    override fun equalTypes(a: KotlinType, b: KotlinType): Boolean =
        createClassicTypeCheckerState(
            false, kotlinTypeRefiner = kotlinTypeRefiner, kotlinTypePreparator = kotlinTypePreparator
        ).equalTypes(a.unwrap(), b.unwrap())

    fun TypeCheckerState.equalTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractTypeChecker.equalTypes(this, a, b)
    }

    fun TypeCheckerState.isSubtypeOf(subType: UnwrappedType, superType: UnwrappedType): Boolean {
        return AbstractTypeChecker.isSubtypeOf(this, subType, superType)
    }
}

object NullabilityChecker {
    fun isSubtypeOfAny(type: UnwrappedType): Boolean =
        SimpleClassicTypeSystemContext
            .newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
            .hasNotNullSupertype(type.lowerIfFlexible(), SupertypesPolicy.LowerIfFlexible)
}

fun UnwrappedType.hasSupertypeWithGivenTypeConstructor(typeConstructor: TypeConstructor) =
    createClassicTypeCheckerState(isErrorTypeEqualsToAnything = false).anySupertype(lowerIfFlexible(), {
        require(it is SimpleType)
        it.constructor == typeConstructor
    }, { SupertypesPolicy.LowerIfFlexible })

fun UnwrappedType.anySuperTypeConstructor(predicate: (TypeConstructor) -> Boolean) =
    createClassicTypeCheckerState(isErrorTypeEqualsToAnything = false).anySupertype(lowerIfFlexible(), {
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

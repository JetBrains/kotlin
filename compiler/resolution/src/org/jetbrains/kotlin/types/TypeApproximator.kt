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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration.IntersectionStrategy.*
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.checker.CaptureStatus.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isNullableAny


open class TypeApproximatorConfiguration {
    enum class IntersectionStrategy {
        ALLOWED,
        TO_FIRST,
        TO_COMMON_SUPERTYPE
    }
    open val flexible get() = false // simple flexible types (FlexibleTypeImpl)
    open val dynamic get() = false // DynamicType
    open val rawType get() = false // RawTypeImpl
    open val errorType get() = false
    open val intersection: IntersectionStrategy = TO_COMMON_SUPERTYPE

    open val typeVariable: (TypeVariableTypeConstructor) -> Boolean = { false }
    open val capturedType: (NewCapturedType) -> Boolean = { false } // true means that this type we can leave as is

    abstract class AllFlexibleSameValue : TypeApproximatorConfiguration() {
        abstract val allFlexible: Boolean

        override val flexible get() = allFlexible
        override val dynamic get() = allFlexible
        override val rawType get() = allFlexible
    }

    object LocalDeclaration : AllFlexibleSameValue() {
        override val allFlexible get() = true
        override val intersection get() = ALLOWED
        override val errorType get() = true
    }

    object PublicDeclaration : AllFlexibleSameValue() {
        override val allFlexible get() = true
        override val errorType get() = true
    }

    abstract class AbstractCapturedTypesApproximation(val approximatedCapturedStatus: CaptureStatus): TypeApproximatorConfiguration.AllFlexibleSameValue() {
        override val allFlexible get() = true

        // i.e. will be approximated only approximatedCapturedStatus captured types
        override val capturedType get() = { it: NewCapturedType -> it.captureStatus != approximatedCapturedStatus }
        override val intersection get() = IntersectionStrategy.ALLOWED
        override val typeVariable: (TypeVariableTypeConstructor) -> Boolean get() = { true }
    }

    object IncorporationConfiguration : TypeApproximatorConfiguration.AbstractCapturedTypesApproximation(FOR_INCORPORATION)
    object SubtypeCapturedTypesApproximation : TypeApproximatorConfiguration.AbstractCapturedTypesApproximation(FOR_SUBTYPING)
    object CapturedTypesApproximation : TypeApproximatorConfiguration.AbstractCapturedTypesApproximation(FROM_EXPRESSION)
}

class TypeApproximator {
    private val referenceApproximateToSuperType = this::approximateSimpleToSuperType
    private val referenceApproximateToSubType = this::approximateSimpleToSubType

    fun approximateDeclarationType(baseType: KotlinType, local: Boolean, languageVersionSettings: LanguageVersionSettings): UnwrappedType {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) return baseType.unwrap()

        val configuration = if (local) TypeApproximatorConfiguration.LocalDeclaration else TypeApproximatorConfiguration.PublicDeclaration
        return approximateToSuperType(baseType.unwrap(), configuration) ?: baseType.unwrap()
    }

    // null means that this input type is the result, i.e. input type not contains not-allowed kind of types
    // type <: resultType
    fun approximateToSuperType(type: UnwrappedType, conf: TypeApproximatorConfiguration): UnwrappedType? =
            approximateToSuperType(type, conf, - type.typeDepth())

    // resultType <: type
    fun approximateToSubType(type: UnwrappedType, conf: TypeApproximatorConfiguration): UnwrappedType? =
            approximateToSubType(type, conf, - type.typeDepth())

    private fun approximateToSuperType(type: UnwrappedType, conf: TypeApproximatorConfiguration, depth: Int): UnwrappedType? {
        if (type is TypeUtils.SpecialType) return null
        return approximateTo(NewKotlinTypeChecker.transformToNewType(type), conf, FlexibleType::upperBound,
                             referenceApproximateToSuperType, depth)
    }

    private fun approximateToSubType(type: UnwrappedType, conf: TypeApproximatorConfiguration, depth: Int): UnwrappedType? {
        if (type is TypeUtils.SpecialType) return null
        return approximateTo(NewKotlinTypeChecker.transformToNewType(type), conf, FlexibleType::lowerBound,
                             referenceApproximateToSubType, depth)
    }

    // comments for case bound = upperBound, approximateTo = toSuperType
    private fun approximateTo(
            type: UnwrappedType,
            conf: TypeApproximatorConfiguration,
            bound: FlexibleType.() -> SimpleType,
            approximateTo: (SimpleType, TypeApproximatorConfiguration, depth: Int) -> UnwrappedType?,
            depth: Int
    ): UnwrappedType? {
        when (type) {
            is SimpleType -> return approximateTo(type, conf, depth)
            is FlexibleType -> {
                if (type is DynamicType) {
                    return if (conf.dynamic) null else type.bound()
                }
                else if (type is RawType) {
                    return if (conf.rawType) null else type.bound()
                }

                assert(type is FlexibleTypeImpl) {
                    "Unexpected subclass of FlexibleType: ${type::class.java.canonicalName}, type = $type"
                }

                if (conf.flexible) {
                    /**
                     * Let inputType = L_1..U_1; resultType = L_2..U_2
                     * We should create resultType such as inputType <: resultType.
                     * It means that if A <: inputType, then A <: U_1. And, because inputType <: resultType,
                     * A <: resultType => A <: U_2. I.e. for every type A such A <: U_1, A <: U_2 => U_1 <: U_2.
                     *
                     * Similar for L_1 <: L_2: Let B : resultType <: B. L_2 <: B and L_1 <: B.
                     * I.e. for every type B such as L_2 <: B, L_1 <: B. For example B = L_2.
                     */

                    val lowerResult = approximateTo(type.lowerBound, conf, depth)
                    val upperResult = approximateTo(type.upperBound, conf, depth)
                    if (lowerResult == null && upperResult == null) return null

                    /**
                     * If C <: L..U then C <: L.
                     * inputType.lower <: lowerResult => inputType.lower <: lowerResult?.lowerIfFlexible()
                     * i.e. this type is correct. We use this type, because this type more flexible.
                     *
                     * If U_1 <: U_2.lower .. U_2.upper, then we know only that U_1 <: U_2.upper.
                     */
                    return KotlinTypeFactory.flexibleType(lowerResult?.lowerIfFlexible() ?: type.lowerBound,
                                            upperResult?.upperIfFlexible() ?: type.upperBound)
                }
                else {
                    return type.bound().let { approximateTo(it, conf, depth) ?: it }
                }
            }
        }
    }

    private fun approximateIntersectionType(type: SimpleType, conf: TypeApproximatorConfiguration, toSuper: Boolean, depth: Int): UnwrappedType? {
        val typeConstructor = type.constructor
        assert(typeConstructor is IntersectionTypeConstructor) {
            "Should be intersection type: $type, typeConstructor class: ${typeConstructor::class.java.canonicalName}"
        }
        assert(typeConstructor.supertypes.isNotEmpty()) {
            "Supertypes for intersection type should not be empty: $type"
        }

        var thereIsApproximation = false
        val newTypes = typeConstructor.supertypes.map {
            val newType = if (toSuper) approximateToSuperType(it.unwrap(), conf, depth) else approximateToSubType(it.unwrap(), conf, depth)
            if (newType != null) {
                thereIsApproximation = true
                newType
            } else it.unwrap()
        }

        /**
         * For case ALLOWED:
         * A <: A', B <: B' => A & B <: A' & B'
         *
         * For other case -- it's impossible to find some type except Nothing as subType for intersection type.
         */
        val baseResult = when (conf.intersection) {
            ALLOWED -> if (!thereIsApproximation) return null else intersectTypes(newTypes)
            TO_FIRST -> if (toSuper) newTypes.first() else return type.defaultResult(toSuper = false)
            // commonSupertypeCalculator should handle flexible types correctly
            TO_COMMON_SUPERTYPE -> if (toSuper) NewCommonSuperTypeCalculator.commonSuperType(newTypes) else return type.defaultResult(toSuper = false)
        }

        return if (type.isMarkedNullable) baseResult.makeNullableAsSpecified(true) else baseResult
    }

    private fun approximateCapturedType(type: NewCapturedType, conf: TypeApproximatorConfiguration, toSuper: Boolean, depth: Int): UnwrappedType? {
        val supertypes = type.constructor.supertypes
        val baseSuperType = when (supertypes.size) {
            0 -> type.builtIns.nullableAnyType // Let C = in Int, then superType for C and C? is Any?
            1 -> supertypes.single()
            else -> intersectTypes(supertypes)
        }
        val baseSubType = type.lowerType ?: type.builtIns.nothingType

        if (conf.capturedType(type)) {
            /**
             * Here everything is ok if bounds for this captured type should not be approximated.
             * But. If such bounds contains some unauthorized types, then we cannot leave this captured type "as is".
             * And we cannot create new capture type, because meaning of new captured type is not clear.
             * So, we will just approximate such types
             *
             * todo handle flexible types
             */
            if (approximateToSuperType(baseSuperType, conf, depth) == null && approximateToSubType(baseSubType, conf, depth) == null) {
                return null
            }
        }
        val baseResult = if (toSuper) approximateToSuperType(baseSuperType, conf, depth) ?: baseSuperType else approximateToSubType(baseSubType, conf, depth) ?: baseSubType

        // C = in Int, Int <: C => Int? <: C?
        // C = out Number, C <: Number => C? <: Number?
        return if (type.isMarkedNullable) baseResult.makeNullableAsSpecified(true) else baseResult
    }

    private fun approximateSimpleToSuperType(type: SimpleType, conf: TypeApproximatorConfiguration, depth: Int) =
            approximateTo(type, conf, toSuper = true, depth = depth)
    private fun approximateSimpleToSubType(type: SimpleType, conf: TypeApproximatorConfiguration, depth: Int) =
            approximateTo(type, conf, toSuper = false, depth = depth)

    private fun approximateTo(type: SimpleType, conf: TypeApproximatorConfiguration, toSuper: Boolean, depth: Int): UnwrappedType? {
        if (type.isError) {
            // todo -- fix builtIns. Now builtIns here is DefaultBuiltIns
            return if (conf.errorType) null else type.defaultResult(toSuper)
        }

        if (depth > 3) return type.defaultResult(toSuper)

        if (type.arguments.isNotEmpty()) {
            return approximateParametrizedType(type, conf, toSuper, depth + 1)
        }

        val typeConstructor = type.constructor

        if (typeConstructor is NewCapturedTypeConstructor) {
            assert(type is NewCapturedType) { // KT-16147
                "Type is inconsistent -- somewhere we create type with typeConstructor = $typeConstructor " +
                "and class: ${type::class.java.canonicalName}. type.toString() = $type"
            }
            return approximateCapturedType(type as NewCapturedType, conf, toSuper, depth)
        }

        if (typeConstructor is IntersectionTypeConstructor) {
            return approximateIntersectionType(type, conf, toSuper, depth)
        }

        if (typeConstructor is TypeVariableTypeConstructor) {
            return if (conf.typeVariable(typeConstructor)) null else type.defaultResult(toSuper)
        }

        return null // simple classifier type
    }

    private fun isApproximateDirectionToSuper(effectiveVariance: Variance, toSuper: Boolean) =
            when (effectiveVariance) {
                Variance.OUT_VARIANCE -> toSuper
                Variance.IN_VARIANCE -> !toSuper
                Variance.INVARIANT -> throw AssertionError("Incorrect variance $effectiveVariance")
            }

    private fun approximateParametrizedType(type: SimpleType, conf: TypeApproximatorConfiguration, toSuper: Boolean, depth: Int): SimpleType? {
        val parameters = type.constructor.parameters
        val arguments = type.arguments
        if (parameters.size != arguments.size) {
            return if (conf.errorType) {
                ErrorUtils.createErrorType("Inconsistent type: $type (parameters.size = ${parameters.size}, arguments.size = ${arguments.size})")
            }
            else type.defaultResult(toSuper)
        }

        val newArguments = arrayOfNulls<TypeProjection?>(arguments.size)

        loop@ for (index in arguments.indices) {
            val parameter = parameters[index]
            val argument = arguments[index]

            if (argument.isStarProjection) continue

            val argumentType = argument.type.unwrap()
            val effectiveVariance = NewKotlinTypeChecker.effectiveVariance(parameter.variance, argument.projectionKind)
            when (effectiveVariance) {
                null -> {
                    return if (conf.errorType) {
                        ErrorUtils.createErrorType("Inconsistent type: $type ($index parameter has declared variance: ${parameter.variance}, " +
                                                   "but argument variance is ${argument.projectionKind})")
                    } else type.defaultResult(toSuper)
                }
                Variance.OUT_VARIANCE, Variance.IN_VARIANCE -> {
                    /**
                     * Out<Foo> <: Out<superType(Foo)>
                     * Inv<out Foo> <: Inv<out superType(Foo)>

                     * In<Foo> <: In<subType(Foo)>
                     * Inv<in Foo> <: Inv<in subType(Foo)>
                     */
                    val approximatedArgument = argumentType.let {
                        if (isApproximateDirectionToSuper(effectiveVariance, toSuper)) {
                            approximateToSuperType(it, conf, depth)
                        }
                        else {
                            approximateToSubType(it, conf, depth)
                        }
                    } ?: continue@loop

                    if (parameter.variance == Variance.INVARIANT) {
                        newArguments[index] = TypeProjectionImpl(effectiveVariance, approximatedArgument)
                    } else {
                        newArguments[index] = approximatedArgument.asTypeProjection()
                    }
                }
                Variance.INVARIANT -> {
                    if (!toSuper) {
                        // Inv<Foo> cannot be approximated to subType
                        val toSubType = approximateToSubType(argumentType, conf, depth) ?: continue@loop

                        // Inv<Foo!> is supertype for Inv<Foo?>
                        if (!NewKotlinTypeChecker.equalTypes(argumentType, toSubType)) return type.defaultResult(toSuper)

                        // also Captured(out Nothing) = Nothing
                        newArguments[index] = toSubType.asTypeProjection()
                        continue@loop
                    }

                    /**
                     * Example with non-trivial both type approximations:
                     * Inv<In<C>> where C = in Int
                     * Inv<In<C>> <: Inv<out In<Int>>
                     * Inv<In<C>> <: Inv<in In<Any?>>
                     *
                     * So such case is rare and we will chose Inv<out In<Int>> for now.
                     *
                     * Note that for case Inv<C> we will chose Inv<in Int>, because it is more informative then Inv<out Any?>.
                     * May be we should do the same for deeper types, but not now.
                     */
                    if (argumentType is NewCapturedType) {
                        val subType = approximateToSubType(argumentType, conf, depth) ?: continue@loop
                        if (!subType.isTrivialSub()) {
                            newArguments[index] = TypeProjectionImpl(Variance.IN_VARIANCE, subType)
                            continue@loop
                        }
                    }

                    val approximatedSuperType = approximateToSuperType(argumentType, conf, depth) ?: continue@loop // null means that this type we can leave as is
                    if (approximatedSuperType.isTrivialSuper()) {
                        val approximatedSubType = approximateToSubType(argumentType, conf, depth) ?: continue@loop // seems like this is never null
                        if (!approximatedSubType.isTrivialSub()) {
                            newArguments[index] = TypeProjectionImpl(Variance.IN_VARIANCE, approximatedSubType)
                            continue@loop
                        }
                    }

                    newArguments[index] = TypeProjectionImpl(Variance.OUT_VARIANCE, approximatedSuperType)
                }
            }
        }

        if (newArguments.all { it == null }) return null

        val newArgumentsList = arguments.mapIndexed { index, oldArgument -> newArguments[index] ?: oldArgument }
        return type.replace(newArgumentsList)
    }

    private fun SimpleType.defaultResult(toSuper: Boolean) = if (toSuper) builtIns.nullableAnyType else {
        if (isMarkedNullable) builtIns.nullableNothingType else builtIns.nothingType
    }

    // Any? or Any!
    private fun UnwrappedType.isTrivialSuper() = upperIfFlexible().isNullableAny()

    // Nothing or Nothing!
    private fun UnwrappedType.isTrivialSub() = lowerIfFlexible().isNothing()
}

internal fun UnwrappedType.typeDepth() =
        when (this) {
            is SimpleType -> typeDepth()
            is FlexibleType -> Math.max(lowerBound.typeDepth(), upperBound.typeDepth())
        }

internal fun SimpleType.typeDepth(): Int {
    if (this is TypeUtils.SpecialType) return 0

    val maxInArguments = arguments.asSequence().map {
        if (it.isStarProjection) 1 else it.type.unwrap().typeDepth()
    }.max() ?: 0

    return maxInArguments + 1
}

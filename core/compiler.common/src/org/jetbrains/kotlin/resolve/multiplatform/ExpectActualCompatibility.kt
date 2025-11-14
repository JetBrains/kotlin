/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility.Mismatch

private const val TYPE_PARAMETER_COUNT = "the number of type parameters is different"
private const val TYPE_PARAMETER_UPPER_BOUNDS = "the upper bounds of type parameters are different"

/**
 * All mismatches that can be fixed by introducing an overload without this mismatch.
 * In other words: "overloadable" mismatches
 *
 * @see ExpectActualIncompatibility
 */
sealed class ExpectActualMatchingCompatibility {
    sealed class Mismatch(val reason: String) : ExpectActualMatchingCompatibility()

    object CallableKind : Mismatch("callable kinds are different (function vs property)")
    object ActualJavaField : Mismatch("actualization to Java field is prohibited")
    object ParameterShape : Mismatch("parameter shapes are different (extension vs non-extension)")
    object ParameterCount : Mismatch("the number of value parameters is different")
    object ContextParameterCount : Mismatch("the number of context parameters is different")
    object FunctionTypeParameterCount : Mismatch(TYPE_PARAMETER_COUNT)
    object ParameterTypes : Mismatch("parameter types are different")
    object ParameterNames : Mismatch("parameter names are different")
    object ContextParameterTypes : Mismatch("context parameter types are different")
    object FunctionTypeParameterUpperBounds : Mismatch(TYPE_PARAMETER_UPPER_BOUNDS)
    object MatchedSuccessfully : ExpectActualMatchingCompatibility()
}

/**
 * "Non-overloadable" compatibilities
 *
 * @see ExpectActualMatchingCompatibility
 */
sealed class ExpectActualIncompatibility<out D>(open val reason: String) {
    object ClassTypeParameterCount : ExpectActualIncompatibility<Nothing>(TYPE_PARAMETER_COUNT)

    // Callables
    object ReturnType : ExpectActualIncompatibility<Nothing>("the return types are different")
    object ParameterNames : ExpectActualIncompatibility<Nothing>("the parameter names are different")
    object ContextParameterNames : ExpectActualIncompatibility<Nothing>("the context parameter names are different")
    object TypeParameterNames : ExpectActualIncompatibility<Nothing>("the names of type parameters are different")
    object ValueParameterVararg :
        ExpectActualIncompatibility<Nothing>("some value parameter is vararg in one declaration and non-vararg in the other")

    object ValueParameterNoinline :
        ExpectActualIncompatibility<Nothing>("some value parameter is noinline in one declaration and not noinline in the other")

    object ValueParameterCrossinline :
        ExpectActualIncompatibility<Nothing>("some value parameter is crossinline in one declaration and not crossinline in the other")

    // Reason is not really required because the renderer for ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT doesn't use it, but it is here just in case.
    object IgnorabilityIsDifferent : ExpectActualIncompatibility<Nothing>("") {
        override val reason: Nothing get() = error("This incompatibility should be reported with ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT diagnostic")
    }

    // Functions
    object FunctionModifiersDifferent : ExpectActualIncompatibility<Nothing>("the modifiers are different (suspend)")
    object FunctionModifiersNotSubset :
        ExpectActualIncompatibility<Nothing>("some modifiers on 'expect' declaration are missing on the 'actual' one (infix, inline, operator)")

    object ActualFunctionWithOptionalParameters :
        ExpectActualIncompatibility<Nothing>("the 'actual' function cannot have parameters with default values, they should be declared in the 'expect' function")

    object ParametersWithDefaultValuesInExpectActualizedByFakeOverride :
        ExpectActualIncompatibility<Nothing>("default parameter values inside 'expect' declaration are not allowed for methods actualized via fake override")

    // Properties
    object PropertyKind : ExpectActualIncompatibility<Nothing>("the property kinds are different (val vs var)")
    object PropertyLateinitModifier : ExpectActualIncompatibility<Nothing>("the modifiers are different (lateinit)")
    object PropertyConstModifier : ExpectActualIncompatibility<Nothing>("the modifiers are different (const)")
    object PropertySetterVisibility : ExpectActualIncompatibility<Nothing>("the setter visibilities are different")

    // Classifiers
    object ClassKind :
        ExpectActualIncompatibility<Nothing>("the class kinds are different (class, interface, object, enum, annotation)")

    object ClassModifiers : ExpectActualIncompatibility<Nothing>("the modifiers are different (companion, inner, inline, value)")
    object FunInterfaceModifier :
        ExpectActualIncompatibility<Nothing>("the 'actual' declaration for 'fun expect interface' is not a functional interface")

    object Supertypes : ExpectActualIncompatibility<Nothing>("some supertypes are missing in the 'actual' declaration")
    object NestedTypeAlias : ExpectActualIncompatibility<Nothing>("actualization by nested type alias is prohibited")
    class ClassScopes<D>(
        val mismatchedMembers: List<Pair</* expect */ D, Map<Mismatch, /* actuals */ Collection<D>>>>,
        val incompatibleMembers: List<MemberIncompatibility<D>>,
    ) : ExpectActualIncompatibility<D>("some 'expect' members have no 'actual' ones")

    object EnumEntries : ExpectActualIncompatibility<Nothing>("some entries from 'expect enum' are missing in the 'actual enum'")
    object IllegalRequiresOpt :
        ExpectActualIncompatibility<Nothing>("opt-in annotations are prohibited to be 'expect' or 'actual'. Instead, declare annotation once in common sources")

    // Common
    class Modality(
        expectModality: org.jetbrains.kotlin.descriptors.Modality?,
        actualModality: org.jetbrains.kotlin.descriptors.Modality?,
    ) : ExpectActualIncompatibility<Nothing>(
        "the modalities are different ('${expectModality.toString().lowercase()}' vs '${actualModality.toString().lowercase()}')"
    )

    object Visibility : ExpectActualIncompatibility<Nothing>("the visibilities are different")

    object ClassTypeParameterUpperBounds : ExpectActualIncompatibility<Nothing>(TYPE_PARAMETER_UPPER_BOUNDS)
    object TypeParameterVariance :
        ExpectActualIncompatibility<Nothing>("declaration-site variances of type parameters are different")

    object TypeParameterReified :
        ExpectActualIncompatibility<Nothing>("some type parameter is reified in one declaration and non-reified in the other")
}

data class MemberIncompatibility<out D>(
    val expect: D,
    val actual: D,
    val incompatibility: ExpectActualIncompatibility<D>,
)

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility.Mismatch

private const val TYPE_PARAMETER_COUNT = "number of type parameters is different"
private const val TYPE_PARAMETER_UPPER_BOUNDS = "upper bounds of type parameters are different"

// Note that the reason is used in the diagnostic output, see PlatformIncompatibilityDiagnosticRenderer
/**
 * DON'T USE THIS CLASS. This class is currently used only in diagnostics. Eventually, it will go away KT-62631
 */
sealed interface ExpectActualCompatibility<out D> {
    /**
     * DON'T USE THIS CLASS. This class is currently used only in diagnostics. Eventually, it will go away KT-62631
     */
    sealed interface MismatchOrIncompatible<out D> : ExpectActualCompatibility<D> {
        val reason: String?
    }
}

/**
 * All mismatches that can be fixed by introducing an overload without this mismatch.
 * In other words: "overloadable" mismatches
 *
 * @see ExpectActualCheckingCompatibility
 */
sealed class ExpectActualMatchingCompatibility : ExpectActualCompatibility<Nothing> {
    sealed class Mismatch(override val reason: String?) : ExpectActualMatchingCompatibility(),
        ExpectActualCompatibility.MismatchOrIncompatible<Nothing>

    object CallableKind : Mismatch("callable kinds are different (function vs property)")
    object ActualJavaField : Mismatch("actualization to Java field is prohibited")
    object ParameterShape : Mismatch("parameter shapes are different (extension vs non-extension)")
    object ParameterCount : Mismatch("number of value parameters is different")
    object FunctionTypeParameterCount : Mismatch(TYPE_PARAMETER_COUNT)
    object ParameterTypes : Mismatch("parameter types are different")
    object FunctionTypeParameterUpperBounds : Mismatch(TYPE_PARAMETER_UPPER_BOUNDS)
    object MatchedSuccessfully : ExpectActualMatchingCompatibility()
}

/**
 * "Non-overloadable" compatibilities
 *
 * @see ExpectActualMatchingCompatibility
 */
sealed class ExpectActualCheckingCompatibility<out D> : ExpectActualCompatibility<D> {
    sealed class Incompatible<out D>(override val reason: String?) : ExpectActualCheckingCompatibility<D>(),
        ExpectActualCompatibility.MismatchOrIncompatible<D>

    object ClassTypeParameterCount : Incompatible<Nothing>(TYPE_PARAMETER_COUNT)

    // Callables
    object ReturnType : Incompatible<Nothing>("return type is different")
    object ParameterNames : Incompatible<Nothing>("parameter names are different")
    object TypeParameterNames : Incompatible<Nothing>("names of type parameters are different")
    object ValueParameterVararg : Incompatible<Nothing>("some value parameter is vararg in one declaration and non-vararg in the other")
    object ValueParameterNoinline :
        Incompatible<Nothing>("some value parameter is noinline in one declaration and not noinline in the other")
    object ValueParameterCrossinline :
        Incompatible<Nothing>("some value parameter is crossinline in one declaration and not crossinline in the other")

    // Functions
    object FunctionModifiersDifferent : Incompatible<Nothing>("modifiers are different (suspend)")
    object FunctionModifiersNotSubset :
        Incompatible<Nothing>("some modifiers on expected declaration are missing on the actual one (infix, inline, operator)")
    object ActualFunctionWithDefaultParameters :
        Incompatible<Nothing>("actual function cannot have default argument values, they should be declared in the expected function")
    object DefaultArgumentsInExpectActualizedByFakeOverride :
        Incompatible<Nothing>("default argument values inside expect declaration are not allowed for methods actualized via fake override")

    // Properties
    object PropertyKind : Incompatible<Nothing>("property kinds are different (val vs var)")
    object PropertyLateinitModifier : Incompatible<Nothing>("modifiers are different (lateinit)")
    object PropertyConstModifier : Incompatible<Nothing>("modifiers are different (const)")
    object PropertySetterVisibility : Incompatible<Nothing>("setter visibility is different")

    // Classifiers
    object ClassKind : Incompatible<Nothing>("class kinds are different (class, interface, object, enum, annotation)")
    object ClassModifiers : Incompatible<Nothing>("modifiers are different (companion, inner, inline, value)")
    object FunInterfaceModifier : Incompatible<Nothing>("actual declaration for fun expect interface is not a functional interface")
    object Supertypes : Incompatible<Nothing>("some supertypes are missing in the actual declaration")
    class ClassScopes<D>(
        val mismatchedMembers: List<Pair</* expect */ D, Map<Mismatch, /* actuals */ Collection<D>>>>,
        val incompatibleMembers: List<Pair</* expect */ D, Map<Incompatible<D>, /* actuals */ Collection<D>>>>,
    ) : Incompatible<D>("some expected members have no actual ones")
    object EnumEntries : Incompatible<Nothing>("some entries from expected enum are missing in the actual enum")

    // Common
    object Modality : Incompatible<Nothing>("modality is different")
    object Visibility : Incompatible<Nothing>("visibility is different")

    object ClassTypeParameterUpperBounds : Incompatible<Nothing>(TYPE_PARAMETER_UPPER_BOUNDS)
    object TypeParameterVariance : Incompatible<Nothing>("declaration-site variances of type parameters are different")
    object TypeParameterReified : Incompatible<Nothing>("some type parameter is reified in one declaration and non-reified in the other")
    object Compatible : ExpectActualCheckingCompatibility<Nothing>()
}

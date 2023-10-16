/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility.Mismatch

// todo cleanup "strong" "weak" comments
// todo cleanup 'out D' generic

private const val TYPE_PARAMETER_COUNT = "number of type parameters is different"
private const val TYPE_PARAMETER_UPPER_BOUNDS = "upper bounds of type parameters are different"

// Note that the reason is used in the diagnostic output, see PlatformIncompatibilityDiagnosticRenderer
sealed interface ExpectActualCompatibility<out D> { // todo try to drop // todo convert to class
    sealed interface MismatchOrIncompatible<out D> : ExpectActualCompatibility<D> { // todo drop
        val reason: String?
    }
}

// For StrongIncompatible `actual` declaration is considered as overload and error reports on expected declaration
sealed class ExpectActualMatchingCompatibility<out D> : ExpectActualCompatibility<D> {
    sealed class Mismatch<out D>(override val reason: String?) : ExpectActualMatchingCompatibility<D>(),
        ExpectActualCompatibility.MismatchOrIncompatible<D>

    // Callables
    object CallableKind : Mismatch<Nothing>("callable kinds are different (function vs property)")

    object ParameterShape : Mismatch<Nothing>("parameter shapes are different (extension vs non-extension)")

    object ParameterCount : Mismatch<Nothing>("number of value parameters is different")

    // FunctionTypeParameterCount is strong because functions can be overloaded by type parameter count
    object FunctionTypeParameterCount : Mismatch<Nothing>(TYPE_PARAMETER_COUNT)

    object ParameterTypes : Mismatch<Nothing>("parameter types are different")

    // FunctionTypeParameterUpperBounds is weak because functions can be overloaded by type parameter upper bounds
    object FunctionTypeParameterUpperBounds : Mismatch<Nothing>(TYPE_PARAMETER_UPPER_BOUNDS)

    object MatchedSuccessfully : ExpectActualMatchingCompatibility<Nothing>()
}

sealed class ExpectActualCheckingCompatibility<out D> : ExpectActualCompatibility<D> {
    sealed class Incompatible<out D>(override val reason: String?) : ExpectActualCheckingCompatibility<D>(),
        ExpectActualCompatibility.MismatchOrIncompatible<D>

    object ClassTypeParameterCount : Incompatible<Nothing>(TYPE_PARAMETER_COUNT)

    // Callables
    object ReturnType : Incompatible<Nothing>("return type is different")
    object ParameterNames : Incompatible<Nothing>("parameter names are different")
    object TypeParameterNames : Incompatible<Nothing>("names of type parameters are different")
    object ValueParameterVararg : Incompatible<Nothing>("some value parameter is vararg in one declaration and non-vararg in the other")
    object ValueParameterNoinline : Incompatible<Nothing>(
        "some value parameter is noinline in one declaration and not noinline in the other"
    )

    object ValueParameterCrossinline : Incompatible<Nothing>(
        "some value parameter is crossinline in one declaration and not crossinline in the other"
    )

    // Functions
    object FunctionModifiersDifferent : Incompatible<Nothing>("modifiers are different (suspend)")
    object FunctionModifiersNotSubset : Incompatible<Nothing>(
        "some modifiers on expected declaration are missing on the actual one (infix, inline, operator)"
    )
    object ActualFunctionWithDefaultParameters :
        Incompatible<Nothing>("actual function cannot have default argument values, they should be declared in the expected function")

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
    // todo add KDoc?
    class ClassScopes<D>(
        val mismatchedMembers: List<Pair<D, Map<Mismatch<D>, Collection<D>>>>,
        val incompatibleMembers: List<Pair<D, Map<Incompatible<D>, Collection<D>>>>,
    ) : Incompatible<D>("some expected members have no actual ones")
    object EnumEntries : Incompatible<Nothing>("some entries from expected enum are missing in the actual enum")

    // Common
    object Modality : Incompatible<Nothing>("modality is different")
    object Visibility : Incompatible<Nothing>("visibility is different")

    object ClassTypeParameterUpperBounds : Incompatible<Nothing>(TYPE_PARAMETER_UPPER_BOUNDS)
    object TypeParameterVariance : Incompatible<Nothing>("declaration-site variances of type parameters are different")
    object TypeParameterReified : Incompatible<Nothing>(
        "some type parameter is reified in one declaration and non-reified in the other"
    )
    object Compatible : ExpectActualCheckingCompatibility<Nothing>()
}

//val ExpectActualCompatibility<*>.isCompatibleOrWeaklyIncompatible: Boolean // todo cleanup usages
//    get() = this is ExpectActualCompatibility.Compatible
//            || this is ExpectActualCompatibility.Incompatible.ExpectActualCheckingCompatibility

//val ExpectActualCompatibility<*>.compatible: Boolean
//    get() = this == ExpectActualCompatibility.Compatible

//@OptIn(ExperimentalContracts::class)
//fun ExpectActualCompatibility<*>.isIncompatible(): Boolean {
//    contract {
//        returns(true) implies (this@isIncompatible is ExpectActualCompatibility.Incompatible<*>)
//    }
//    return !compatible
//}

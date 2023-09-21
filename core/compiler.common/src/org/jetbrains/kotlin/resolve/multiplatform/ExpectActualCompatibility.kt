/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class ExpectActualCompatibility<out D> {

    // Note that the reason is used in the diagnostic output, see PlatformIncompatibilityDiagnosticRenderer
    sealed class Incompatible<out D>(val reason: String?) : ExpectActualCompatibility<D>() {

        sealed class ExpectActualCheckingIncompatible<out D>(reason: String?) : Incompatible<D>(reason)

        // For StrongIncompatible `actual` declaration is considered as overload and error reports on expected declaration
        sealed class ExpectActualMatchingIncompatible<out D>(reason: String?) : Incompatible<D>(reason)

        // Callables

        object CallableKind : ExpectActualMatchingIncompatible<Nothing>("callable kinds are different (function vs property)")

        object ParameterShape : ExpectActualMatchingIncompatible<Nothing>("parameter shapes are different (extension vs non-extension)")

        object ParameterCount : ExpectActualMatchingIncompatible<Nothing>("number of value parameters is different")

        // FunctionTypeParameterCount is strong because functions can be overloaded by type parameter count
        object FunctionTypeParameterCount : ExpectActualMatchingIncompatible<Nothing>("number of type parameters is different")

        // ClassTypeParameterCount is weak because classes cannot be overloaded
        object ClassTypeParameterCount : ExpectActualCheckingIncompatible<Nothing>(FunctionTypeParameterCount.reason)

        object ParameterTypes : ExpectActualMatchingIncompatible<Nothing>("parameter types are different")
        object ReturnType : ExpectActualCheckingIncompatible<Nothing>("return type is different")

        object ParameterNames : ExpectActualCheckingIncompatible<Nothing>("parameter names are different")
        object TypeParameterNames : ExpectActualCheckingIncompatible<Nothing>("names of type parameters are different")

        object ValueParameterVararg : ExpectActualCheckingIncompatible<Nothing>("some value parameter is vararg in one declaration and non-vararg in the other")
        object ValueParameterNoinline : ExpectActualCheckingIncompatible<Nothing>(
            "some value parameter is noinline in one declaration and not noinline in the other"
        )

        object ValueParameterCrossinline : ExpectActualCheckingIncompatible<Nothing>(
            "some value parameter is crossinline in one declaration and not crossinline in the other"
        )

        // Functions

        object FunctionModifiersDifferent : ExpectActualCheckingIncompatible<Nothing>("modifiers are different (suspend)")
        object FunctionModifiersNotSubset : ExpectActualCheckingIncompatible<Nothing>(
            "some modifiers on expected declaration are missing on the actual one (infix, inline, operator)"
        )
        object ActualFunctionWithDefaultParameters :
            ExpectActualCheckingIncompatible<Nothing>("actual function cannot have default argument values, they should be declared in the expected function")

        // Properties

        object PropertyKind : ExpectActualCheckingIncompatible<Nothing>("property kinds are different (val vs var)")
        object PropertyLateinitModifier : ExpectActualCheckingIncompatible<Nothing>("modifiers are different (lateinit)")
        object PropertyConstModifier : ExpectActualCheckingIncompatible<Nothing>("modifiers are different (const)")
        object PropertySetterVisibility : ExpectActualCheckingIncompatible<Nothing>("setter visibility is different")

        // Classifiers

        object ClassKind : ExpectActualCheckingIncompatible<Nothing>("class kinds are different (class, interface, object, enum, annotation)")

        object ClassModifiers : ExpectActualCheckingIncompatible<Nothing>("modifiers are different (companion, inner, inline, value)")

        object FunInterfaceModifier : ExpectActualCheckingIncompatible<Nothing>("actual declaration for fun expect interface is not a functional interface")

        object Supertypes : ExpectActualCheckingIncompatible<Nothing>("some supertypes are missing in the actual declaration")

        class ClassScopes<D>(
            val unfulfilled: List<Pair<D, Map<Incompatible<D>, Collection<D>>>>
        ) : ExpectActualCheckingIncompatible<D>("some expected members have no actual ones")

        object EnumEntries : ExpectActualCheckingIncompatible<Nothing>("some entries from expected enum are missing in the actual enum")

        // Common

        object Modality : ExpectActualCheckingIncompatible<Nothing>("modality is different")
        object Visibility : ExpectActualCheckingIncompatible<Nothing>("visibility is different")

        // FunctionTypeParameterUpperBounds is weak because functions can be overloaded by type parameter upper bounds
        object FunctionTypeParameterUpperBounds : ExpectActualMatchingIncompatible<Nothing>("upper bounds of type parameters are different")

        // ClassTypeParameterUpperBounds is strong because classes cannot be overloaded
        object ClassTypeParameterUpperBounds : ExpectActualCheckingIncompatible<Nothing>(FunctionTypeParameterUpperBounds.reason)

        object TypeParameterVariance : ExpectActualCheckingIncompatible<Nothing>("declaration-site variances of type parameters are different")
        object TypeParameterReified : ExpectActualCheckingIncompatible<Nothing>(
            "some type parameter is reified in one declaration and non-reified in the other"
        )
    }

    object Compatible : ExpectActualCompatibility<Nothing>()
}

val ExpectActualCompatibility<*>.isCompatibleOrWeaklyIncompatible: Boolean
    get() = this is ExpectActualCompatibility.Compatible
            || this is ExpectActualCompatibility.Incompatible.ExpectActualCheckingIncompatible

val ExpectActualCompatibility<*>.compatible: Boolean
    get() = this == ExpectActualCompatibility.Compatible

@OptIn(ExperimentalContracts::class)
fun ExpectActualCompatibility<*>.isIncompatible(): Boolean {
    contract {
        returns(true) implies (this@isIncompatible is ExpectActualCompatibility.Incompatible<*>)
    }
    return !compatible
}

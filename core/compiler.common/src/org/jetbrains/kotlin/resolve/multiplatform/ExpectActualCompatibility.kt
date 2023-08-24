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

        sealed class WeakIncompatible<out D>(reason: String?) : Incompatible<D>(reason)

        // For StrongIncompatible `actual` declaration is considered as overload and error reports on expected declaration
        sealed class StrongIncompatible<out D>(reason: String?) : Incompatible<D>(reason)

        // Callables

        object CallableKind : StrongIncompatible<Nothing>("callable kinds are different (function vs property)")

        object ParameterShape : StrongIncompatible<Nothing>("parameter shapes are different (extension vs non-extension)")

        object ParameterCount : StrongIncompatible<Nothing>("number of value parameters is different")

        // FunctionTypeParameterCount is strong because functions can be overloaded by type parameter count
        object FunctionTypeParameterCount : StrongIncompatible<Nothing>("number of type parameters is different")

        // ClassTypeParameterCount is weak because classes cannot be overloaded
        object ClassTypeParameterCount : WeakIncompatible<Nothing>(FunctionTypeParameterCount.reason)

        object ParameterTypes : StrongIncompatible<Nothing>("parameter types are different")
        object ReturnType : StrongIncompatible<Nothing>("return type is different")

        object ParameterNames : WeakIncompatible<Nothing>("parameter names are different")
        object TypeParameterNames : WeakIncompatible<Nothing>("names of type parameters are different")

        object ValueParameterVararg : WeakIncompatible<Nothing>("some value parameter is vararg in one declaration and non-vararg in the other")
        object ValueParameterNoinline : WeakIncompatible<Nothing>(
            "some value parameter is noinline in one declaration and not noinline in the other"
        )

        object ValueParameterCrossinline : WeakIncompatible<Nothing>(
            "some value parameter is crossinline in one declaration and not crossinline in the other"
        )

        // Functions

        object FunctionModifiersDifferent : WeakIncompatible<Nothing>("modifiers are different (suspend)")
        object FunctionModifiersNotSubset : WeakIncompatible<Nothing>(
            "some modifiers on expected declaration are missing on the actual one (infix, inline, operator)"
        )
        object ActualFunctionWithDefaultParameters :
            WeakIncompatible<Nothing>("actual function cannot have default argument values, they should be declared in the expected function")

        // Properties

        object PropertyKind : WeakIncompatible<Nothing>("property kinds are different (val vs var)")
        object PropertyLateinitModifier : WeakIncompatible<Nothing>("modifiers are different (lateinit)")
        object PropertyConstModifier : WeakIncompatible<Nothing>("modifiers are different (const)")
        object PropertySetterVisibility : WeakIncompatible<Nothing>("setter visibility is different")

        // Classifiers

        object ClassKind : WeakIncompatible<Nothing>("class kinds are different (class, interface, object, enum, annotation)")

        object ClassModifiers : WeakIncompatible<Nothing>("modifiers are different (companion, inner, inline)")

        object FunInterfaceModifier : WeakIncompatible<Nothing>("actual declaration for fun expect interface is not a functional interface")

        object Supertypes : WeakIncompatible<Nothing>("some supertypes are missing in the actual declaration")

        class ClassScopes<D>(
            val unfulfilled: List<Pair<D, Map<Incompatible<D>, Collection<D>>>>
        ) : WeakIncompatible<D>("some expected members have no actual ones")

        object EnumEntries : WeakIncompatible<Nothing>("some entries from expected enum are missing in the actual enum")

        // Common

        object Modality : WeakIncompatible<Nothing>("modality is different")
        object Visibility : WeakIncompatible<Nothing>("visibility is different")

        // FunctionTypeParameterUpperBounds is weak because functions can be overloaded by type parameter upper bounds
        object FunctionTypeParameterUpperBounds : StrongIncompatible<Nothing>("upper bounds of type parameters are different")

        // ClassTypeParameterUpperBounds is strong because classes cannot be overloaded
        object ClassTypeParameterUpperBounds : WeakIncompatible<Nothing>(FunctionTypeParameterUpperBounds.reason)

        object TypeParameterVariance : WeakIncompatible<Nothing>("declaration-site variances of type parameters are different")
        object TypeParameterReified : WeakIncompatible<Nothing>(
            "some type parameter is reified in one declaration and non-reified in the other"
        )
    }

    object Compatible : ExpectActualCompatibility<Nothing>()
}

val ExpectActualCompatibility<*>.compatible: Boolean
    get() = this == ExpectActualCompatibility.Compatible

@OptIn(ExperimentalContracts::class)
fun ExpectActualCompatibility<*>.isIncompatible(): Boolean {
    contract {
        returns(true) implies (this@isIncompatible is ExpectActualCompatibility.Incompatible<*>)
    }
    return !compatible
}

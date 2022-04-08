/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

sealed class ExpectActualCompatibility<out D> {
    // For IncompatibilityKind.STRONG `actual` declaration is considered as overload and error reports on expected declaration
    enum class IncompatibilityKind {
        WEAK, STRONG
    }

    // Note that the reason is used in the diagnostic output, see PlatformIncompatibilityDiagnosticRenderer
    sealed class Incompatible<out D>(
        val reason: String?,
        val kind: IncompatibilityKind = IncompatibilityKind.WEAK
    ) : ExpectActualCompatibility<D>() {
        // Callables

        object CallableKind : Incompatible<Nothing>(
            "callable kinds are different (function vs property)",
            IncompatibilityKind.STRONG
        )

        object ParameterShape : Incompatible<Nothing>(
            "parameter shapes are different (extension vs non-extension)",
            IncompatibilityKind.STRONG
        )

        object ParameterCount : Incompatible<Nothing>("number of value parameters is different", IncompatibilityKind.STRONG)
        object TypeParameterCount : Incompatible<Nothing>("number of type parameters is different", IncompatibilityKind.STRONG)

        object ParameterTypes : Incompatible<Nothing>("parameter types are different", IncompatibilityKind.STRONG)
        object ReturnType : Incompatible<Nothing>("return type is different", IncompatibilityKind.STRONG)

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
            "some modifiers on expected declaration are missing on the actual one (external, infix, inline, operator, tailrec)"
        )

        // Properties

        object PropertyKind : Incompatible<Nothing>("property kinds are different (val vs var)")
        object PropertyLateinitModifier : Incompatible<Nothing>("modifiers are different (lateinit)")
        object PropertyConstModifier : Incompatible<Nothing>("modifiers are different (const)")

        // Classifiers

        object ClassKind : Incompatible<Nothing>("class kinds are different (class, interface, object, enum, annotation)")

        object ClassModifiers : Incompatible<Nothing>("modifiers are different (companion, inner, inline)")

        object Supertypes : Incompatible<Nothing>("some supertypes are missing in the actual declaration")

        class ClassScopes<D>(
            val unfulfilled: List<Pair<D, Map<Incompatible<D>, Collection<D>>>>
        ) : Incompatible<D>("some expected members have no actual ones")

        object EnumEntries : Incompatible<Nothing>("some entries from expected enum are missing in the actual enum")

        // Common

        object Modality : Incompatible<Nothing>("modality is different")
        object Visibility : Incompatible<Nothing>("visibility is different")

        object TypeParameterUpperBounds : Incompatible<Nothing>("upper bounds of type parameters are different", IncompatibilityKind.STRONG)
        object TypeParameterVariance : Incompatible<Nothing>("declaration-site variances of type parameters are different")
        object TypeParameterReified : Incompatible<Nothing>(
            "some type parameter is reified in one declaration and non-reified in the other"
        )

        object Unknown : Incompatible<Nothing>(null)
    }

    object Compatible : ExpectActualCompatibility<Nothing>()
}

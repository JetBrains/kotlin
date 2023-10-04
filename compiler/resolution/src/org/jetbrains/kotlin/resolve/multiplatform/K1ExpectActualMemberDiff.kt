/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

// This class will be later used by K2. That's why it's placed in compiler.common module
data class K1ExpectActualMemberDiff<out M, out C>(val kind: Kind, val actualMember: M, val expectClass: C) {
    /**
     * Diff kinds that are legal for fake-overrides in final `expect class`, but illegal for non-final `expect class`
     *
     * Also see: [toMemberDiffKind]
     */
    enum class Kind(val rawMessage: String) {
        NonPrivateCallableAdded(
            "{0}: non-private member must be declared in both the actual class and the expect class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
        ReturnTypeChangedInOverride(
            "{0}: the return type of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
        ModalityChangedInOverride(
            "{0}: the modality of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
        VisibilityChangedInOverride(
            "{0}: the visibility of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
        SetterVisibilityChangedInOverride(
            "{0}: the setter visibility of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
        ParameterNameChangedInOverride(
            "{0}: the parameter names of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
        PropertyKindChangedInOverride(
            "{0}: the property kind (val vs var) of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
        LateinitChangedInOverride(
            "{0}: the property modifiers (lateinit) of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
        TypeParameterNamesChangedInOverride(
            "{0}: the type parameter names of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
    }
}

fun K1ExpectActualCompatibility.Incompatible<*>.toMemberDiffKind(): K1ExpectActualMemberDiff.Kind? = when (this) {
    K1ExpectActualCompatibility.Incompatible.CallableKind -> K1ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    K1ExpectActualCompatibility.Incompatible.ParameterCount -> K1ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    K1ExpectActualCompatibility.Incompatible.ParameterShape -> K1ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    K1ExpectActualCompatibility.Incompatible.ParameterTypes -> K1ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    K1ExpectActualCompatibility.Incompatible.ReturnType -> K1ExpectActualMemberDiff.Kind.ReturnTypeChangedInOverride
    K1ExpectActualCompatibility.Incompatible.FunctionTypeParameterCount -> K1ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    K1ExpectActualCompatibility.Incompatible.ClassTypeParameterCount -> error("Not applicable because K1ExpectActualMemberDiff is about members")
    K1ExpectActualCompatibility.Incompatible.FunctionTypeParameterUpperBounds -> K1ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    K1ExpectActualCompatibility.Incompatible.ClassTypeParameterUpperBounds -> error("Not applicable because K1ExpectActualMemberDiff is about members")
    K1ExpectActualCompatibility.Incompatible.ActualFunctionWithDefaultParameters -> null // It's not possible to add default parameters in override
    K1ExpectActualCompatibility.Incompatible.ClassKind -> error("Not applicable because K1ExpectActualMemberDiff is about members")
    K1ExpectActualCompatibility.Incompatible.ClassModifiers -> error("Not applicable because K1ExpectActualMemberDiff is about members")
    is K1ExpectActualCompatibility.Incompatible.ClassScopes -> error("Not applicable because K1ExpectActualMemberDiff is about members")
    K1ExpectActualCompatibility.Incompatible.EnumEntries -> error("Not applicable because K1ExpectActualMemberDiff is about members")
    K1ExpectActualCompatibility.Incompatible.FunInterfaceModifier -> error("Not applicable because K1ExpectActualMemberDiff is about members")
    K1ExpectActualCompatibility.Incompatible.FunctionModifiersDifferent -> null // It's not possible to override with different function modifier (suspend)
    K1ExpectActualCompatibility.Incompatible.FunctionModifiersNotSubset -> null // It's not possible to override with different function modifier (infix, inline, operator)
    K1ExpectActualCompatibility.Incompatible.Modality -> K1ExpectActualMemberDiff.Kind.ModalityChangedInOverride
    K1ExpectActualCompatibility.Incompatible.ParameterNames -> K1ExpectActualMemberDiff.Kind.ParameterNameChangedInOverride
    K1ExpectActualCompatibility.Incompatible.PropertyConstModifier -> null // const fun can't be overridden
    K1ExpectActualCompatibility.Incompatible.PropertyKind -> K1ExpectActualMemberDiff.Kind.PropertyKindChangedInOverride
    K1ExpectActualCompatibility.Incompatible.PropertyLateinitModifier -> K1ExpectActualMemberDiff.Kind.LateinitChangedInOverride
    K1ExpectActualCompatibility.Incompatible.PropertySetterVisibility -> K1ExpectActualMemberDiff.Kind.SetterVisibilityChangedInOverride
    K1ExpectActualCompatibility.Incompatible.Supertypes -> error("Not applicable because K1ExpectActualMemberDiff is about members")
    K1ExpectActualCompatibility.Incompatible.TypeParameterNames -> K1ExpectActualMemberDiff.Kind.TypeParameterNamesChangedInOverride
    K1ExpectActualCompatibility.Incompatible.TypeParameterReified -> null // inline fun can't be overridden
    K1ExpectActualCompatibility.Incompatible.TypeParameterVariance -> null // Members are not allowed to have variance
    K1ExpectActualCompatibility.Incompatible.ValueParameterCrossinline -> null // inline fun can't be overridden
    K1ExpectActualCompatibility.Incompatible.ValueParameterNoinline -> null // inline fun can't be overridden
    K1ExpectActualCompatibility.Incompatible.ValueParameterVararg -> K1ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    K1ExpectActualCompatibility.Incompatible.Visibility -> K1ExpectActualMemberDiff.Kind.VisibilityChangedInOverride
}

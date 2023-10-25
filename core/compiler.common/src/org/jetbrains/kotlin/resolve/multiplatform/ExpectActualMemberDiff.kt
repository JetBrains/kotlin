/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

// This class will be later used by K2. That's why it's placed in compiler.common module
data class ExpectActualMemberDiff<out M, out C>(val kind: Kind, val actualMember: M, val expectClass: C) {
    /**
     * Diff kinds that are legal for fake-overrides in final `expect class`, but illegal for non-final `expect class`
     *
     * Also see: [toMemberDiffKind]
     */
    enum class Kind(val rawMessage: String) {
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
        VarargChangedInOverride(
            "{0}: the parameter modifiers (vararg) of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
        TypeParameterNamesChangedInOverride(
            "{0}: the type parameter names of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        ),
        Unknown(
            "{0}: normally, this error should never happen. Please report to https://kotl.in/issue. " +
                    "This error happens because the expect class ''{1}'' is non-final"
        )
    }
}

fun ExpectActualCheckingCompatibility.Incompatible<*>.toMemberDiffKind(): ExpectActualMemberDiff.Kind? = when (this) {
    ExpectActualCheckingCompatibility.ReturnType -> ExpectActualMemberDiff.Kind.ReturnTypeChangedInOverride
    ExpectActualCheckingCompatibility.ClassTypeParameterCount -> error("Not applicable because ExpectActualMemberDiff is about members")
    ExpectActualCheckingCompatibility.ClassTypeParameterUpperBounds -> error("Not applicable because ExpectActualMemberDiff is about members")
    ExpectActualCheckingCompatibility.ActualFunctionWithDefaultParameters -> null // It's not possible to add default parameters in override
    ExpectActualCheckingCompatibility.ClassKind -> error("Not applicable because ExpectActualMemberDiff is about members")
    ExpectActualCheckingCompatibility.ClassModifiers -> error("Not applicable because ExpectActualMemberDiff is about members")
    is ExpectActualCheckingCompatibility.ClassScopes -> error("Not applicable because ExpectActualMemberDiff is about members")
    ExpectActualCheckingCompatibility.EnumEntries -> error("Not applicable because ExpectActualMemberDiff is about members")
    ExpectActualCheckingCompatibility.FunInterfaceModifier -> error("Not applicable because ExpectActualMemberDiff is about members")
    ExpectActualCheckingCompatibility.FunctionModifiersDifferent -> null // It's not possible to override with different function modifier (suspend)
    ExpectActualCheckingCompatibility.FunctionModifiersNotSubset -> null // It's not possible to override with different function modifier (infix, inline, operator)
    ExpectActualCheckingCompatibility.Modality -> ExpectActualMemberDiff.Kind.ModalityChangedInOverride
    ExpectActualCheckingCompatibility.ParameterNames -> ExpectActualMemberDiff.Kind.ParameterNameChangedInOverride
    ExpectActualCheckingCompatibility.PropertyConstModifier -> null // const fun can't be overridden
    ExpectActualCheckingCompatibility.PropertyKind -> ExpectActualMemberDiff.Kind.PropertyKindChangedInOverride
    ExpectActualCheckingCompatibility.PropertyLateinitModifier -> ExpectActualMemberDiff.Kind.LateinitChangedInOverride
    ExpectActualCheckingCompatibility.PropertySetterVisibility -> ExpectActualMemberDiff.Kind.SetterVisibilityChangedInOverride
    ExpectActualCheckingCompatibility.Supertypes -> error("Not applicable because ExpectActualMemberDiff is about members")
    ExpectActualCheckingCompatibility.TypeParameterNames -> ExpectActualMemberDiff.Kind.TypeParameterNamesChangedInOverride
    ExpectActualCheckingCompatibility.TypeParameterReified -> null // inline fun can't be overridden
    ExpectActualCheckingCompatibility.TypeParameterVariance -> null // Members are not allowed to have variance
    ExpectActualCheckingCompatibility.ValueParameterCrossinline -> null // inline fun can't be overridden
    ExpectActualCheckingCompatibility.ValueParameterNoinline -> null // inline fun can't be overridden
    ExpectActualCheckingCompatibility.ValueParameterVararg -> ExpectActualMemberDiff.Kind.VarargChangedInOverride
    ExpectActualCheckingCompatibility.Visibility -> ExpectActualMemberDiff.Kind.VisibilityChangedInOverride
}

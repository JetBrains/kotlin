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
     * Also see: [toDiffKind]
     */
    enum class Kind(val rawMessage: String) {
        NonPrivateCallableAdded(
            "{0}: non-private member must be declared in the expect class as well. " +
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

/**
 * This function serves a purpose of type-safe documentation. A mapping can be established between [ExpectActualCompatibility.Incompatible]
 * and [ExpectActualMemberDiff.Kind]. This exhaustive when fixates this mapping, ensuring that we won't forget to add new
 * [ExpectActualMemberDiff.Kind] when [ExpectActualCompatibility.Incompatible] is updated.
 */
@Suppress("unused") // The function is a documentation. That's why it's unused
private fun ExpectActualCompatibility.Incompatible<*>.toDiffKind(): ExpectActualMemberDiff.Kind? = when (this) {
    ExpectActualCompatibility.Incompatible.CallableKind -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    ExpectActualCompatibility.Incompatible.ParameterCount -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    ExpectActualCompatibility.Incompatible.ParameterShape -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    ExpectActualCompatibility.Incompatible.ParameterTypes -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    ExpectActualCompatibility.Incompatible.ReturnType -> ExpectActualMemberDiff.Kind.ReturnTypeChangedInOverride
    ExpectActualCompatibility.Incompatible.FunctionTypeParameterCount -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    ExpectActualCompatibility.Incompatible.ClassTypeParameterCount -> null // Not applicable because ExpectActualMemberDiff is about members
    ExpectActualCompatibility.Incompatible.FunctionTypeParameterUpperBounds -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    ExpectActualCompatibility.Incompatible.ClassTypeParameterUpperBounds -> null // Not applicable because ExpectActualMemberDiff is about members
    ExpectActualCompatibility.Incompatible.ActualFunctionWithDefaultParameters -> null // It's not possible to add default parameters in override
    ExpectActualCompatibility.Incompatible.ClassKind -> null // Not applicable because ExpectActualMemberDiff is about members
    ExpectActualCompatibility.Incompatible.ClassModifiers -> null // Not applicable because ExpectActualMemberDiff is about members
    is ExpectActualCompatibility.Incompatible.ClassScopes -> null // Not applicable because ExpectActualMemberDiff is about members
    ExpectActualCompatibility.Incompatible.EnumEntries -> null // Not applicable because ExpectActualMemberDiff is about members
    ExpectActualCompatibility.Incompatible.FunInterfaceModifier -> null // Not applicable because ExpectActualMemberDiff is about members
    ExpectActualCompatibility.Incompatible.FunctionModifiersDifferent -> null // It's not possible to override with different function modifier
    ExpectActualCompatibility.Incompatible.FunctionModifiersNotSubset -> null // It's not possible to override with different function modifier
    ExpectActualCompatibility.Incompatible.Modality -> ExpectActualMemberDiff.Kind.ModalityChangedInOverride
    ExpectActualCompatibility.Incompatible.ParameterNames -> ExpectActualMemberDiff.Kind.ParameterNameChangedInOverride
    ExpectActualCompatibility.Incompatible.PropertyConstModifier -> null // const fun can't be overridden
    ExpectActualCompatibility.Incompatible.PropertyKind -> ExpectActualMemberDiff.Kind.PropertyKindChangedInOverride
    ExpectActualCompatibility.Incompatible.PropertyLateinitModifier -> ExpectActualMemberDiff.Kind.LateinitChangedInOverride
    ExpectActualCompatibility.Incompatible.PropertySetterVisibility -> ExpectActualMemberDiff.Kind.SetterVisibilityChangedInOverride
    ExpectActualCompatibility.Incompatible.Supertypes -> null // Not applicable because ExpectActualMemberDiff is about members. But related: ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER
    ExpectActualCompatibility.Incompatible.TypeParameterNames -> ExpectActualMemberDiff.Kind.TypeParameterNamesChangedInOverride
    ExpectActualCompatibility.Incompatible.TypeParameterReified -> null // inline fun can't be overridden
    ExpectActualCompatibility.Incompatible.TypeParameterVariance -> null // Members are not allowed to have variance
    ExpectActualCompatibility.Incompatible.ValueParameterCrossinline -> null // inline fun can't be overridden
    ExpectActualCompatibility.Incompatible.ValueParameterNoinline -> null // inline fun can't be overridden
    ExpectActualCompatibility.Incompatible.ValueParameterVararg -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    ExpectActualCompatibility.Incompatible.Visibility -> ExpectActualMemberDiff.Kind.VisibilityChangedInOverride
}

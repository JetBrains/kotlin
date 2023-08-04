/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

data class ExpectActualMemberDiff<out M, out C>(val kind: Kind, val actualMember: M, val expectClass: C) {
    /**
     * Diff kinds that are legal for fake-overrides in final `expect class`, but illegal for non-final `expect class`
     *
     * Also see: [toDiffKind]
     */
    enum class Kind(val rawMessage: String) {
        NonPrivateCallableAdded(
            "{0}: Non-private member must be declared in the expect class as well. " +
                    "This error happens because the expect class ''{1}'' is non-final."
        ),

        ReturnTypeCovariantOverride(
            "{0}: The return type of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final."
        ),

        ModalityChangedInOverride(
            "{0}: The modality of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final."
        ),

        VisibilityChangedInOverride(
            "{0}: The visibility of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final."
        ),

        ParameterNameChangedInOverride(
            "{0}: The parameter names of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final."
        ),

        PropertyKindChangedInOverride(
            "{0}: The property kind (val vs var) of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final."
        ),
    }
}

/**
 * This function serves a purpose of type-safe documentation. A mapping can be established between [ExpectActualCompatibility.Incompatible]
 * and [ExpectActualMemberDiff.Kind]. This exhaustive when fixates this mapping, ensuring that we won't forget to add new
 * [ExpectActualMemberDiff.Kind] when [ExpectActualCompatibility.Incompatible] is updated and vice versa.
 */
@Suppress("unused")
private fun ExpectActualCompatibility.Incompatible<*>.toDiffKind(): ExpectActualMemberDiff.Kind? {
    return when (this) {
        ExpectActualCompatibility.Incompatible.CallableKind -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
        ExpectActualCompatibility.Incompatible.ParameterCount -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
        ExpectActualCompatibility.Incompatible.ParameterShape -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
        ExpectActualCompatibility.Incompatible.ParameterTypes -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
        ExpectActualCompatibility.Incompatible.ReturnType -> ExpectActualMemberDiff.Kind.ReturnTypeCovariantOverride
        ExpectActualCompatibility.Incompatible.TypeParameterCount -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
        ExpectActualCompatibility.Incompatible.TypeParameterUpperBounds -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
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
        ExpectActualCompatibility.Incompatible.PropertyConstModifier -> null // const can't be overridden
        ExpectActualCompatibility.Incompatible.PropertyKind -> ExpectActualMemberDiff.Kind.PropertyKindChangedInOverride
        ExpectActualCompatibility.Incompatible.PropertyLateinitModifier -> TODO()
        ExpectActualCompatibility.Incompatible.PropertySetterVisibility -> TODO()
        ExpectActualCompatibility.Incompatible.Supertypes -> null // Not applicable because ExpectActualMemberDiff is about members. But related: ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER
        ExpectActualCompatibility.Incompatible.TypeParameterNames -> TODO()
        ExpectActualCompatibility.Incompatible.TypeParameterReified -> TODO()
        ExpectActualCompatibility.Incompatible.TypeParameterVariance -> TODO()
        ExpectActualCompatibility.Incompatible.Unknown -> TODO()
        ExpectActualCompatibility.Incompatible.ValueParameterCrossinline -> TODO()
        ExpectActualCompatibility.Incompatible.ValueParameterNoinline -> TODO()
        ExpectActualCompatibility.Incompatible.ValueParameterVararg -> TODO()
        ExpectActualCompatibility.Incompatible.Visibility -> TODO()
    }
}

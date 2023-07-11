/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

data class ExpectActualMemberDiff<out M, out C>(val kind: Kind, val actualMember: M, val expectClass: C) {
    /**
     * Diff kinds that are legal for fake-overrides in final `expect class`, but illegal for non-final `expect class`
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

        // todo type parameters?
    }
}

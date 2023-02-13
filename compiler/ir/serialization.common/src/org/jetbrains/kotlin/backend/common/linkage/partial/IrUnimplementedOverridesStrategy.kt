/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrOverridableMember
import org.jetbrains.kotlin.ir.linkage.partial.IrUnimplementedOverridesStrategy
import org.jetbrains.kotlin.ir.linkage.partial.PartiallyLinkedDeclarationOrigin

internal object ImplementAsErrorThrowingStubs : IrUnimplementedOverridesStrategy {
    override fun <T : IrOverridableMember> computeCustomization(overridableMember: T, parent: IrClass) =
        if (overridableMember.modality == Modality.ABSTRACT
            && parent.modality != Modality.ABSTRACT
            && parent.modality != Modality.SEALED
        ) {
            IrUnimplementedOverridesStrategy.Customization(
                origin = PartiallyLinkedDeclarationOrigin.UNIMPLEMENTED_ABSTRACT_CALLABLE_MEMBER,
                modality = parent.modality // Use modality of class for implemented callable member.
            )
        } else
            IrUnimplementedOverridesStrategy.Customization.NO
}

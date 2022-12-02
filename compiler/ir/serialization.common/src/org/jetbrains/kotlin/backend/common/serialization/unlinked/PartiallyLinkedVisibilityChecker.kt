/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.util.nameForIrSerialization

internal object PartiallyLinkedVisibilityChecker {
    fun checkVisibilityInFile(declaration: IrDeclarationWithVisibility, file: PartialLinkageUtils.File): Boolean {
        if (declaration.origin == PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION) {
            // There is no information about declaration visibility. Also, such declarations
            // are detected as linkage errors anyways.
            return true
        }

        when (val visibility = declaration.visibility) {
            DescriptorVisibilities.PUBLIC, DescriptorVisibilities.PROTECTED -> return true
            DescriptorVisibilities.INTERNAL -> TODO()
            DescriptorVisibilities.PRIVATE -> TODO()
            else -> error("Unexpected visibility $visibility in declaration $declaration, ${declaration.nameForIrSerialization}")
        }
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility

@PrettyIrDsl
interface IrDeclarationWithVisibilityBuilder {

    var declarationVisibility: DescriptorVisibility

    @IrNodePropertyDsl
    fun visibility(visibility: DescriptorVisibility) {
        declarationVisibility = visibility
    }

    @IrNodePropertyDsl
    fun visibilityPrivate() {
        visibility(DescriptorVisibilities.PRIVATE)
    }

    @IrNodePropertyDsl
    fun visibilityPrivateToThis() {
        visibility(DescriptorVisibilities.PRIVATE_TO_THIS)
    }

    @IrNodePropertyDsl
    fun visibilityProtected() {
        visibility(DescriptorVisibilities.PROTECTED)
    }

    @IrNodePropertyDsl
    fun visibilityInternal() {
        visibility(DescriptorVisibilities.INTERNAL)
    }

    @IrNodePropertyDsl
    fun visibilityPublic() {
        visibility(DescriptorVisibilities.PUBLIC)
    }

    @IrNodePropertyDsl
    fun visibilityLocal() {
        visibility(DescriptorVisibilities.LOCAL)
    }

    @IrNodePropertyDsl
    fun visibilityInherited() {
        visibility(DescriptorVisibilities.INHERITED)
    }

    @IrNodePropertyDsl
    fun visibilityInvisibleFake() {
        visibility(DescriptorVisibilities.INVISIBLE_FAKE)
    }

    @IrNodePropertyDsl
    fun visibilityUnknown() {
        visibility(DescriptorVisibilities.UNKNOWN)
    }
}

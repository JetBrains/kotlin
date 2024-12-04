/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.ClassId

/**
 * Makes sure that all encountered [IrField]s are private unless they participate in Java interop.
 */
internal object IrFieldVisibilityChecker : IrFieldChecker {
    private val JVM_FIELD_CLASS_ID = ClassId.fromString("kotlin/jvm/JvmField")

    // TODO: Some backing fields inherit their visibility from their corresponding properties.
    //   We disable validation for such properties until KT-71243 is resolved.
    private val IrField.isExemptFromValidation: Boolean
        get() = correspondingPropertySymbol?.owner?.isConst == true ||
                hasAnnotation(JVM_FIELD_CLASS_ID)

    override fun check(
        declaration: IrField,
        context: CheckerContext,
    ) {
        if (declaration.visibility != DescriptorVisibilities.PRIVATE && !declaration.isExemptFromValidation) {
            context.error(declaration, "Kotlin fields are expected to always be private")
        }
    }
}
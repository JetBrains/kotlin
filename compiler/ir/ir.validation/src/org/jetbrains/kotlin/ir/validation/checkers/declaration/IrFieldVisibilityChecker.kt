/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.name.ClassId

/**
 * Makes sure that all encountered [IrField]s are private unless they participate in Java interop.
 */
object IrFieldVisibilityChecker : IrElementChecker<IrField>(IrField::class) {
    private val JVM_FIELD_CLASS_ID = ClassId.fromString("kotlin/jvm/JvmField")

    // TODO: Some backing fields inherit their visibility from their corresponding properties.
    //   We disable validation for such properties until KT-71243 is resolved.
    private val IrField.isExemptFromValidation: Boolean
        get() = correspondingPropertySymbol?.owner?.isConst == true ||
                hasAnnotation(JVM_FIELD_CLASS_ID)

    override fun check(element: IrField, context: CheckerContext) {
        if (element.visibility != DescriptorVisibilities.PRIVATE && !element.isExemptFromValidation) {
            context.error(element, "Kotlin fields are expected to always be private")
        }
    }
}
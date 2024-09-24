/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId

/**
 * Makes sure that:
 * - [IrField]s are not accessed outside their containing files.
 * - All encountered [IrField]s are private unless they participate in Java interop.
 */
internal class IrFieldValidator(
    private val file: IrFile,
    private val config: IrValidatorConfig,
    private val reportError: ReportIrValidationError,
) : IrElementVisitorVoid {
    private val parentChain = mutableListOf<IrElement>()

    companion object {
        private val JVM_FIELD_CLASS_ID = ClassId.fromString("kotlin/jvm/JvmField")
    }

    override fun visitElement(element: IrElement) {
        parentChain.push(element)
        element.acceptChildrenVoid(this)
        parentChain.pop()
    }

    // TODO: Some backing fields inherit their visibility from their corresponding properties.
    //   We disable validation for such properties until KT-71243 is resolved.
    private val IrField.isExemptFromValidation: Boolean
        get() = correspondingPropertySymbol?.owner?.isConst == true ||
                hasAnnotation(JVM_FIELD_CLASS_ID)

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)
        if (!config.checkAllKotlinFieldsArePrivate) return

        if (declaration.visibility != DescriptorVisibilities.PRIVATE && !declaration.isExemptFromValidation) {
            reportError(file, declaration, "Kotlin fields are expected to always be private", parentChain)
        }
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression) {
        super.visitFieldAccess(expression)
        if (!config.checkCrossFileFieldUsage) return

        val field = expression.symbol.owner
        if (field.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return
        val containingFile = field.fileOrNull ?: return

        if (containingFile != file) {
            reportError(file, expression, "Access to a field declared in another file: ${containingFile.path}", parentChain)
        }
    }
}

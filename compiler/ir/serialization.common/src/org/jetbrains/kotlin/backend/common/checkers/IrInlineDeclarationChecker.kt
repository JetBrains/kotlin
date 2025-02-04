/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers

import org.jetbrains.kotlin.backend.common.checkers.IrInlineDeclarationChecker.InlineFunctionInfo
import org.jetbrains.kotlin.backend.common.diagnostics.SerializationErrors
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.overrides.isNonPrivate
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.parents
import org.jetbrains.kotlin.ir.visitors.IrTypeVisitor

/**
 * Reports an IR-level diagnostic whenever a private type is used within an `inline` function with broader visibility.
 * TODO: move class to :compiler:ir:ir.inline when no longer invoked from :compiler:ir.serialization.common
 */
class IrInlineDeclarationChecker(
    private val diagnosticReporter: IrDiagnosticReporter,
) : IrTypeVisitor<Unit, InlineFunctionInfo?>() {

    data class InlineFunctionInfo(
        val file: IrFile,
        val insideEffectivelyPrivateDeclaration: Boolean = false,
        val inlineFunction: IrFunction? = null,
        val inliningPath: List<IrInlinedFunctionBlock> = listOf()
    ) {
        fun insideDeclaration(declaration: IrDeclaration, inlineFunction: IrFunction? = this.inlineFunction): InlineFunctionInfo {
            if (this.inlineFunction != null) return this
            if (declaration !is IrDeclarationWithVisibility) return this
            return InlineFunctionInfo(
                file,
                insideEffectivelyPrivateDeclaration || !declaration.isNonPrivate,
                inlineFunction,
            )
        }
    }

    override fun visitType(container: IrElement, type: IrType, data: InlineFunctionInfo?) {
        val inlineFunction = data?.inlineFunction ?: return
        val klass = type.classifierOrNull?.takeIf { it.isBound }?.owner as? IrClass ?: return
        if (!data.insideEffectivelyPrivateDeclaration &&
            klass.isEffectivelyPrivate() &&
            klass.parents.none { it == inlineFunction } // local/private classed declared in the current inline function are legal.
        ) {
            if (data.inliningPath.isNotEmpty()) {
                diagnosticReporter.at(data.inliningPath.first(), data.file).report(
                    SerializationErrors.IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING,
                    inlineFunction,
                    klass,
                    data.inliningPath
                )
            } else {
                diagnosticReporter.at(container, data.file).report(
                    SerializationErrors.IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION,
                    inlineFunction,
                    klass,
                )
            }
        }
    }

    override fun visitElement(element: IrElement, data: InlineFunctionInfo?) {
        element.acceptChildren(this, data)
    }

    override fun visitFile(declaration: IrFile, data: InlineFunctionInfo?) {
        declaration.acceptChildren(this, InlineFunctionInfo(declaration))
    }

    override fun visitClass(declaration: IrClass, data: InlineFunctionInfo?) {
        declaration.acceptChildren(this, data?.insideDeclaration(declaration))
    }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: InlineFunctionInfo?) {
        inlinedBlock.acceptChildren(this, data?.copy(inliningPath = data.inliningPath + inlinedBlock))
    }

    override fun visitFunction(declaration: IrFunction, data: InlineFunctionInfo?) {
        declaration.acceptChildren(this, data?.insideDeclaration(declaration, declaration.takeIf { it.isInline }))
    }
}
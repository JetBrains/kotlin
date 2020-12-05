/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal

// Move static member declarations from classes to top level
class StaticMembersLowering(val context: JsCommonBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        (declaration.parent as? IrClass)?.let { irClass ->
            val isStatic = when (declaration) {
                is IrClass -> !declaration.isEffectivelyExternal()
                is IrSimpleFunction -> declaration.isStaticMethodOfClass && !declaration.isEffectivelyExternal()
                is IrField -> declaration.isStatic
                else -> false
            }

            if (isStatic) {
                // JsExport might be inherited from parent declaration which would be broken if we move it out of its parent.
                // Marking declaration as exported explicitly.
                if (context is JsIrBackendContext && declaration.isExported(context)) {
                    context.additionalExportedDeclarations.add(declaration)
                }
                var extractedUnder = declaration
                var newContainer = declaration.parent
                while (newContainer is IrDeclaration && newContainer != irClass.file) {
                    extractedUnder = newContainer
                    newContainer = newContainer.parent
                }
                val insertBefore = irClass.file.declarations.indexOf(extractedUnder)
                if (insertBefore >= 0) {
                    irClass.file.declarations.add(insertBefore, declaration)
                } else {
                    irClass.file.declarations += declaration
                }
                declaration.parent = irClass.file
                return listOf()
            }
        }

        return null
    }
}
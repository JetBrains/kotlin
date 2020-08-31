/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal

// Move static member declarations from classes to top level
class StaticMembersLowering(val context: JsIrBackendContext) : DeclarationTransformer {
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
                if (declaration.isExported(context)) {
                    context.additionalExportedDeclarations.add(declaration)
                }
                declaration.parent = irClass.file
                irClass.file.declarations += declaration
                return listOf()
            }
        }

        return null
    }
}
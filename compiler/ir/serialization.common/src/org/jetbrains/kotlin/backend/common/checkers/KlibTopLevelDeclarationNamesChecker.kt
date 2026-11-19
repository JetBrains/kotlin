/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers

import org.jetbrains.kotlin.backend.common.diagnostics.SerializationErrors
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

/**
 * Reports top-level classes, functions and properties having a dot in their names.
 * There's no need to run the checker for Native, since FirNativeIdentifierChecker already prohibits the dot in any declaration.
 *
 * [org.jetbrains.kotlin.ir.util.IdSignature.CommonSignature] uses the dot as a separator of the name segments
 * like `MyClass.<init>`, and the linker looks up the top-level declaration by the first segment, so such declarations
 * cannot be linked after deserialization from a KLIB.
 * Dots in names of members, local declarations, enum entries, type aliases, type and value parameters don't break linkage.
 *
 * Only top-level declarations of the module files are inspected, the IR tree is not traversed.
 */
object KlibTopLevelDeclarationNamesChecker {
    private const val DOT_CHAR = '.'

    fun check(module: IrModuleFragment, diagnosticReporter: IrDiagnosticReporter) {
        for (file in module.files) {
            for (declaration in file.declarations) {
                if (declaration !is IrClass && declaration !is IrSimpleFunction && declaration !is IrProperty) continue
                val name = (declaration as IrDeclarationWithName).name
                if (name.isSpecial) continue
                val identifier = name.asString()
                if (DOT_CHAR in identifier) {
                    diagnosticReporter.at(declaration, file).report(SerializationErrors.DOT_IN_TOP_LEVEL_DECLARATION_NAME, identifier)
                }
            }
        }
    }
}

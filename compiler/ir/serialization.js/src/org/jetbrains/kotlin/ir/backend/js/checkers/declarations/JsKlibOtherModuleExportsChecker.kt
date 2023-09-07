/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers.declarations

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.checkers.*

object JsKlibOtherModuleExportsChecker : JsKlibExportedDeclarationsChecker {
    private fun <T> MutableMap<T, MutableList<JsKlibExport>>.addExport(key: T, export: JsKlibExport) {
        getOrPut(key) { mutableListOf() }.add(export)
    }

    private fun collectClashesByFqNames(declarations: List<JsKlibExportingDeclaration>): Map<String, List<JsKlibExport>> {
        return buildMap<String, MutableList<JsKlibExport>> {
            for (declaration in declarations) {
                addExport(declaration.fqName, declaration)

                var packageFqName = declaration.containingPackageFqName
                while (packageFqName.isNotEmpty()) {
                    addExport(packageFqName, JsKlibExportingPackage(declaration.containingFile, packageFqName))

                    packageFqName = packageFqName.substringBeforeLast(".", "")
                }
            }
        }
    }

    private fun collectClashes(declarations: List<JsKlibExportingDeclaration>): Map<JsKlibExportingDeclaration, List<JsKlibExport>> {
        val clashesByFqNames = collectClashesByFqNames(declarations)
        return buildMap {
            for (clashingExports in clashesByFqNames.values) {
                for ((index, export) in clashingExports.withIndex()) {
                    if (export is JsKlibExportingDeclaration) {
                        val clashedWith = clashingExports.filterIndexed { i, _ -> i != index }
                        if (clashedWith.isNotEmpty()) {
                            put(export, clashedWith)
                        }
                    }
                }
            }
        }
    }

    override fun check(declarations: List<JsKlibExportingDeclaration>, reporter: KtDiagnosticReporterWithImplicitIrBasedContext) {
        val clashes = collectClashes(declarations)
        for ((declaration, clashedWith) in clashes) {
            if (declaration.declaration != null) {
                reporter.at(declaration.declaration).report(JsKlibErrors.EXPORTING_JS_NAME_CLASH, declaration.exportingName, clashedWith)
            }
        }
    }
}

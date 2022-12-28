/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds

class FirJsExportedDeclarationNameClashChecker : FirBasicDeclarationChecker() {
    private val nameName = Name.identifier("name")
    private val alreadyUsedExportedNames = mutableMapOf<ExportedName, FirDeclaration>()

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.isLocalMember || declaration.hasJsExportIgnoreAnnotation()) return

        val containingFile = context.containingDeclarations.lastOrNull() as? FirFile

        if (!declaration.hasJsExportAnnotation() && containingFile?.hasJsExportAnnotation() != true) return

        val exportedName = declaration.calculateExportedName()
        val declarationWithTheSameExportedName = alreadyUsedExportedNames[exportedName]

        if (declarationWithTheSameExportedName != null) {
            reporter.reportOn(
                declaration.source,
                FirJsErrors.EXPORTED_NAME_CLASH,
                exportedName.name,
                declarationWithTheSameExportedName.symbol,
                context
            )
            reporter.reportOn(
                declarationWithTheSameExportedName.source,
                FirJsErrors.EXPORTED_NAME_CLASH,
                exportedName.name,
                declaration.symbol,
                context
            )
            return
        }

        alreadyUsedExportedNames[exportedName] = declaration
    }

    private fun FirDeclaration.calculateExportedName(): ExportedName {
        return ExportedName(moduleData.name, getKotlinOrJsName())
    }

    private fun FirDeclaration.hasJsExportAnnotation(): Boolean = hasAnnotation(StandardClassIds.Annotations.JsExport)
    private fun FirDeclaration.hasJsExportIgnoreAnnotation(): Boolean = hasAnnotation(StandardClassIds.Annotations.JsExportIgnore)
    private fun FirDeclaration.getJsNameAnnotationValue(): String? = getAnnotationByClassId(StandardClassIds.Annotations.JsName)?.getStringArgument(nameName)

    private fun FirDeclaration.getKotlinOrJsName(): String {
        return getJsNameAnnotationValue() ?: name?.identifierOrNullIfSpecial ?: error("Expect to have the declaration with an identifier")
    }

    private val FirDeclaration.name: Name?
        get() = when (this) {
            is FirConstructor -> SpecialNames.INIT
            is FirSimpleFunction -> name
            is FirRegularClass -> name
            is FirProperty -> name
            else -> null
        }

    private data class ExportedName(val module: Name, val name: String)
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirErrorImport
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedParentInImport
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile

object FirUnresolvedInMiddleOfImportChecker : FirDeclarationSyntaxChecker<FirFile, KtFile>() {
    override fun checkPsiOrLightTree(element: FirFile, source: KtSourceElement, context: CheckerContext, reporter: DiagnosticReporter) {
        for (import in element.imports) {
            if (import is FirErrorImport) processErrorImport(import, context, reporter)
        }
    }

    private fun processErrorImport(import: FirErrorImport, context: CheckerContext, reporter: DiagnosticReporter) {
        when (val diagnostic = import.diagnostic) {
            is ConeUnresolvedParentInImport -> {
                val source = import.source ?: return
                var segmentSource: KtSourceElement? = source.dotQualifiedExpression() ?: return

                val symbolProvider = context.session.symbolProvider
                val parentClassId = diagnostic.parentClassId

                if (import.isAllUnder && isClassIdPointingToEnumEntry(parentClassId, symbolProvider)) {
                    // Enum entries cannot be resolved as class so star import of enum falls in here and we treat it as
                    // CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON
                    reporter.reportOn(
                        source,
                        FirErrors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON,
                        parentClassId.shortClassName,
                        context,
                    )
                    return
                }

                // For star import, the PSI structure has one less level of qualified references. Hence we start from 0. Otherwise, we start
                // from 1 to skip the last imported name.
                var errorSegmentIndexFromLast = if (import.isAllUnder) 0 else 1
                var currentClassId = parentClassId.parentClassId
                while (currentClassId != null && symbolProvider.getClassLikeSymbolByClassId(currentClassId) == null) {
                    currentClassId = currentClassId.parentClassId
                    errorSegmentIndexFromLast++
                }
                // Finds the right segment from the last that causes resolution to fail. Note that FirImportResolveTransformer always create
                // an error import with resolvable package segments. That is, the segment corresponding to the outermost class name is where
                // resolution failed.
                for (i in 1..errorSegmentIndexFromLast) {
                    segmentSource = segmentSource?.dotQualifiedExpression()
                }
                val unresolvedSource = segmentSource?.selectorExpression() ?: return
                reporter.reportOn(
                    unresolvedSource,
                    FirErrors.UNRESOLVED_IMPORT,
                    parentClassId.getOutermostClassName(),
                    context,
                )
            }
            else -> {
            }
        }
    }

    /**
     * Gets the short outermost class name. For example, `foo.bar.Foo` -> `Foo`. `foo.bar.Outer.Inner.InnerAgain` -> `Outer`.
     */
    private fun ClassId.getOutermostClassName() = relativeClassName.pathSegments().first().asString()

    private fun isClassIdPointingToEnumEntry(classId: ClassId, symbolProvider: FirSymbolProvider): Boolean {
        val enumClassId = classId.parentClassId ?: return false
        val enumClass =
            (symbolProvider.getClassLikeSymbolByClassId(enumClassId) as? FirRegularClassSymbol)?.takeIf { it.isEnumClass } ?: return false
        return enumClass.collectEnumEntries().any { it.callableId.callableName == classId.shortClassName }
    }

    private fun KtSourceElement.dotQualifiedExpression() = getChild(KtNodeTypes.DOT_QUALIFIED_EXPRESSION, depth = 1)
    private fun KtSourceElement.selectorExpression() = getChild(KtNodeTypes.REFERENCE_EXPRESSION, depth = 1, reverse = true)
}

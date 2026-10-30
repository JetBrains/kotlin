/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.requireFeatureSupport
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.forEachChildOfType
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.hasContextParameters
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

object FirContextualFunctionTypeChecker : FirResolvedTypeRefChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: FirResolvedTypeRef) {
        if (typeRef.source?.kind is KtFakeSourceElementKind) return
        if (!typeRef.coneType.abbreviatedTypeOrSelf.hasContextParameters) return

        val source = withSyntax { typeRef.source?.findFunctionTypeContextParameterListSource() }
            ?: errorWithAttachment("Source for type ref of contextual function type doesn't contain context list.") {
                withFirEntry("fir", typeRef)
            }

        source.forEachChildOfType(valueParameterElementSet, depth = 1) {
            reporter.reportOn(it, FirErrors.NAMED_CONTEXT_PARAMETER_IN_FUNCTION_TYPE)
        }

        source.requireFeatureSupport(LanguageFeature.ContextParameters)
    }

    private val valueParameterElementSet = TokenSet.create(KtNodeTypes.VALUE_PARAMETER)
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.references

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.permissions.KaAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisFromWriteAction
import org.jetbrains.kotlin.analysis.api.permissions.allowAnalysisOnEdt
import org.jetbrains.kotlin.idea.references.ReadWriteAccessChecker
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.references.ReferenceAccess
import org.jetbrains.kotlin.types.expressions.OperatorConventions

internal class ReadWriteAccessCheckerDescriptorsImpl : ReadWriteAccessChecker {
    @OptIn(KaAllowAnalysisOnEdt::class, KaAllowAnalysisFromWriteAction::class)
    override fun readWriteAccessWithFullExpressionByResolve(assignment: KtBinaryExpression): Pair<ReferenceAccess, KtExpression> {
        // Various parts of the IJ plugin use the read/write access checker without being aware that it uses `analyze` under the hood. For
        // now, it makes more sense to allow analysis on EDT/from write actions here, as there are many such usages in the IJ plugin.
        return allowAnalysisOnEdt {
            allowAnalysisFromWriteAction {
                val resolvedCall = analyze(assignment) {
                    with((this as KaFe10Session).analysisContext.analyze(assignment, Fe10AnalysisFacade.AnalysisMode.PARTIAL)) {
                        assignment.getCall(this)?.getResolvedCall(this) ?: return ReferenceAccess.READ_WRITE to assignment
                    }
                }
                if (resolvedCall.resultingDescriptor.name in OperatorConventions.ASSIGNMENT_OPERATIONS.values) {
                    ReferenceAccess.READ to assignment
                } else {
                    ReferenceAccess.READ_WRITE to assignment
                }
            }
        }
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtAssignmentNamesProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.extensions.internal.InternalNonStableExtensionPoints
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.references.fe10.base.KtFe10ReferenceResolutionHelper
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.extensions.AssignResolutionAltererExtension

internal class KtFe10AssignmentNamesProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtAssignmentNamesProvider(), Fe10KtAnalysisSessionComponent {

    @OptIn(InternalNonStableExtensionPoints::class)
    override fun getOperationName(expression: KtBinaryExpression): Name? {
        val bindingContext = KtFe10ReferenceResolutionHelper.getInstance().partialAnalyze(expression)
        val lhsType = expression.left?.getType(bindingContext) ?: return null
        val assignAlterers = AssignResolutionAltererExtension.getInstances(expression.project)
        return assignAlterers.firstNotNullOfOrNull { it.getOperationName(expression, lhsType, bindingContext) }
    }
}
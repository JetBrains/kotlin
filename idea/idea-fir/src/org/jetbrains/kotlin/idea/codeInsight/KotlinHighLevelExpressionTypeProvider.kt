/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.idea.frontend.api.analyseInModalWindow
import org.jetbrains.kotlin.idea.frontend.api.types.render
import org.jetbrains.kotlin.psi.KtExpression

class KotlinHighLevelExpressionTypeProvider : KotlinExpressionTypeProvider() {
    override fun KtExpression.shouldShowStatementType(): Boolean {
        return true /* TODO */
    }

    override fun getInformationHint(element: KtExpression): String = analyseInModalWindow(element, "Getting expression type") {
        val rendered = element.getKtType().render()
        StringUtil.escapeXmlEntities(rendered)
    }

    override fun getErrorHint(): String = "No expression found"
}

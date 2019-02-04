/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.generate

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Presentation
import org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateToStringAction
import org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateToStringAction.Generator
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractGenerateToStringActionTest : AbstractCodeInsightActionTest() {
    override fun createAction(fileText: String) = KotlinGenerateToStringAction()

    override fun testAction(action: AnAction, forced: Boolean): Presentation {
        val fileText = file.text
        val generator = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// GENERATOR: ")?.let { Generator.valueOf(it) }
        val generateSuperCall = InTextDirectivesUtils.isDirectiveDefined(fileText, "// GENERATE_SUPER_CALL")
        val klass = file.findElementAt(editor.caretModel.offset)?.getStrictParentOfType<KtClass>()
        try {
            with(KotlinGenerateToStringAction) {
                klass?.adjuster = { it.copy(generateSuperCall = generateSuperCall, generator = generator ?: it.generator) }
            }
            return super.testAction(action, forced)
        } finally {
            with(KotlinGenerateToStringAction) { klass?.adjuster = null }
        }
    }
}

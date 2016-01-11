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

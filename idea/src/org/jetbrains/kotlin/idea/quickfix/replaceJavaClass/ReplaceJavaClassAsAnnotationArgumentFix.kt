/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix.replaceJavaClass

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.util.ArrayList

public class ReplaceJavaClassAsAnnotationArgumentFix(
        annotationEntry: JetAnnotationEntry
) : JetIntentionAction<JetAnnotationEntry>(annotationEntry), CleanupFix {

    override fun getText() = JetBundle.message("replace.java.class.argument")
    override fun getFamilyName() = JetBundle.message("replace.java.class.argument.family")

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        processTasks(createReplacementTasks(element))
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
                diagnostic.createIntentionForFirstParentOfType(::ReplaceJavaClassAsAnnotationArgumentFix)

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory {
            JetWholeProjectForEachElementOfTypeFix.createForMultiTask<JetAnnotationEntry, ReplacementTask>(
                    tasksFactory = { createReplacementTasks(it) },
                    tasksProcessor = ::processTasks,
                    name = "Replace javaClass<T>() with T::class in whole project"
            )
        }
    }
}

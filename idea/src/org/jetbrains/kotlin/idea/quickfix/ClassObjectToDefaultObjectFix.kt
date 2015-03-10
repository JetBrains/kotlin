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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public class ClassObjectToDefaultObjectFix(private val elem: JetObjectDeclaration) : JetIntentionAction<JetObjectDeclaration>(elem) {
    override fun getText(): String = JetBundle.message("migrate.class.object.to.default")

    override fun getFamilyName(): String = JetBundle.message("migrate.class.object.to.default.family")

    override fun invoke(project: Project, editor: Editor, file: JetFile) {
        classKeywordToDefaultModifier(elem)
    }

    default object Factory : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
                (diagnostic.getPsiElement() as? JetObjectDeclaration)?.let { ClassObjectToDefaultObjectFix(it) }

        fun classKeywordToDefaultModifier(objectDeclaration: JetObjectDeclaration) {
            objectDeclaration.getClassKeyword()?.delete()
            if (!objectDeclaration.hasModifier(JetTokens.DEFAULT_KEYWORD)) {
                objectDeclaration.addModifier(JetTokens.DEFAULT_KEYWORD)
            }
        }
    }
}

public class ClassObjectToDefaultObjectInWholeProjectFix(private val elem: JetObjectDeclaration) : JetIntentionAction<JetObjectDeclaration>(elem) {
    override fun getText(): String = JetBundle.message("migrate.class.object.to.default.in.whole.project")

    override fun getFamilyName(): String = JetBundle.message("migrate.class.object.to.default.in.whole.project.family")

    override fun invoke(project: Project, editor: Editor, file: JetFile) {
        val files = PluginJetFilesProvider.allFilesInProject(file.getProject())

        files.forEach { it.accept(ClassObjectToDefaultObjectVisitor) }
    }

    private object ClassObjectToDefaultObjectVisitor : JetTreeVisitorVoid() {
        override fun visitObjectDeclaration(objectDeclaration: JetObjectDeclaration) {
            objectDeclaration.acceptChildren(this)
            if (objectDeclaration.getClassKeyword() != null) {
                ClassObjectToDefaultObjectFix.classKeywordToDefaultModifier(objectDeclaration)
            }
        }
    }

    default object Factory : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
                (diagnostic.getPsiElement() as? JetObjectDeclaration)?.let { ClassObjectToDefaultObjectInWholeProjectFix(it) }
    }
}
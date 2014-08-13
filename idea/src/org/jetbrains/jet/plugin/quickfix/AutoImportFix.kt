/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix

import com.google.common.collect.Lists
import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.ImportPath
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.plugin.actions.JetAddImportAction
import org.jetbrains.jet.plugin.caches.JetShortNamesCache
import org.jetbrains.jet.plugin.caches.KotlinIndicesHelper
import org.jetbrains.jet.plugin.caches.resolve.*
import org.jetbrains.jet.plugin.project.ProjectStructureUtil
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.plugin.util.JetPsiHeuristicsUtil
import java.util.ArrayList
import com.intellij.codeInsight.intention.IntentionAction

/**
 * Check possibility and perform fix for unresolved references.
 */
public class AutoImportFix(element: JetSimpleNameExpression) : JetHintAction<JetSimpleNameExpression>(element), HighPriorityAction {

    private val suggestions: Collection<FqName> = computeSuggestions(element)

    override fun showHint(editor: Editor): Boolean {
        if (suggestions.isEmpty()) return false
        val project = editor.getProject() ?: return false

        if (HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) return false

        if (!ApplicationManager.getApplication()!!.isUnitTestMode()) {
            val hintText = ShowAutoImportPass.getMessage(suggestions.size() > 1, suggestions.first().asString())

            HintManager.getInstance().showQuestionHint(editor, hintText, element.getTextOffset(), element.getTextRange()!!.getEndOffset(), createAction(project, editor))
        }

        return true
    }

    override fun getText()
            = JetBundle.message("import.fix")

    override fun getFamilyName()
            = JetBundle.message("import.fix")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile)
            = super< JetHintAction>.isAvailable(project, editor, file) && !suggestions.isEmpty()

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            createAction(project, editor!!).execute()
        }
    }

    override fun startInWriteAction()
            = true

    private fun createAction(project: Project, editor: Editor)
            = JetAddImportAction(project, editor, element, suggestions)

    private fun computeSuggestions(element: JetSimpleNameExpression): Collection<FqName> {
        val file = element.getContainingFile() as? JetFile ?: return listOf()

        var referenceName = element.getReferencedName()
        if (element.getIdentifier() == null) {
            val conventionName = JetPsiUtil.getConventionName(element)
            if (conventionName != null) {
                referenceName = conventionName.asString()
            }
        }

        if (referenceName.isEmpty()) return listOf()

        val resolveSessionForBodies = element.getLazyResolveSession()

        val module = ModuleUtilCore.findModuleForPsiElement(file) ?: return listOf()
        val searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)

        val result = ArrayList<FqName>()
        if (!isSuppressedTopLevelImportInPosition(element)) {
            result.addAll(getClassNames(referenceName, file, searchScope, resolveSessionForBodies))
            result.addAll(getJetTopLevelFunctions(referenceName, element, searchScope, resolveSessionForBodies, file.getProject()))
        }

        result.addAll(getJetExtensionFunctions(referenceName, element, searchScope, resolveSessionForBodies, file.getProject()))

        return result.filter { ImportInsertHelper.needImport(ImportPath(it, false), file) }
    }

    private fun isSuppressedTopLevelImportInPosition(element: JetSimpleNameExpression)
            = element.isImportDirectiveExpression() || JetPsiUtil.isSelectorInQualified(element)

    private fun getJetTopLevelFunctions(referenceName: String, context: JetExpression, searchScope: GlobalSearchScope, resolveSession: ResolveSessionForBodies, project: Project): Collection<FqName>
            = KotlinIndicesHelper(project).getTopLevelFunctionDescriptorsByName(referenceName, context, resolveSession, searchScope)
            .map { DescriptorUtils.getFqNameSafe(it) }
            .toSet()

    private fun getJetExtensionFunctions(referenceName: String, expression: JetSimpleNameExpression, searchScope: GlobalSearchScope, resolveSession: ResolveSessionForBodies, project: Project): Collection<FqName>
            = KotlinIndicesHelper(project).getCallableExtensions({ it == referenceName}, expression, resolveSession, searchScope)
            .map { DescriptorUtils.getFqNameSafe(it) }
            .toSet()

    /*
     * Searches for possible class names in kotlin context and java facade.
     */
    private fun getClassNames(referenceName: String, file: JetFile, searchScope: GlobalSearchScope, analyzer: KotlinCodeAnalyzer): Collection<FqName> {
        val possibleResolveNames = if (!ProjectStructureUtil.isJsKotlinModule(file)) {
            getClassesFromCache(referenceName, searchScope, file)
        }
        else {
            getJetClasses(referenceName, searchScope, file.getProject(), analyzer)
        }

        // TODO: Do appropriate sorting
        return Lists.newArrayList<FqName>(possibleResolveNames)
    }

    private fun getClassesFromCache(typeName: String, searchScope: GlobalSearchScope, file: JetFile): Collection<FqName>
            = getShortNamesCache(file).getClassesByName(typeName, searchScope)
            .filter { JetPsiHeuristicsUtil.isAccessible(it, file) }
            .map { FqName(it.getQualifiedName()!!) }
            .toSet()

    private fun getShortNamesCache(jetFile: JetFile): PsiShortNamesCache {
        if (ProjectStructureUtil.isJsKotlinModule(jetFile)) {
            return JetShortNamesCache.getKotlinInstance(jetFile.getProject())
        }

        return PsiShortNamesCache.getInstance(jetFile.getProject())
    }

    private fun getJetClasses(typeName: String, searchScope: GlobalSearchScope, project: Project, resolveSession: KotlinCodeAnalyzer): Collection<FqName>
            = KotlinIndicesHelper(project).getClassDescriptorsByName(typeName, resolveSession, searchScope)
            .map { DescriptorUtils.getFqNameSafe(it) }
            .toSet()

    class object {
        public fun createFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): JetIntentionAction<JetSimpleNameExpression>? {
                    // There could be different psi elements (i.e. JetArrayAccessExpression), but we can fix only JetSimpleNameExpression case
                    val psiElement = diagnostic.getPsiElement()
                    if (psiElement is JetSimpleNameExpression) {
                        return AutoImportFix(psiElement)
                    }

                    return null
                }

                override fun isApplicableForCodeFragment()
                        = true
            }
        }
    }
}

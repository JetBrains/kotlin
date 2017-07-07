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

package org.jetbrains.kotlin.idea.injection

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.lang.Language
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTreeUtil.getDeepestLast
import com.intellij.util.Processor
import org.intellij.plugins.intelliLang.Configuration
import org.intellij.plugins.intelliLang.inject.*
import org.intellij.plugins.intelliLang.inject.config.BaseInjection
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.patterns.KotlinPatterns
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset

@NonNls val KOTLIN_SUPPORT_ID = "kotlin"

class KotlinLanguageInjectionSupport : AbstractLanguageInjectionSupport() {
    override fun getId(): String = KOTLIN_SUPPORT_ID

    override fun getPatternClasses() = arrayOf(KotlinPatterns::class.java)

    override fun isApplicableTo(host: PsiLanguageInjectionHost?) = host is KtElement

    override fun useDefaultInjector(host: PsiLanguageInjectionHost?): Boolean = false

    override fun addInjectionInPlace(language: Language?, host: PsiLanguageInjectionHost?): Boolean {
        if (language == null || host == null) return false

        val configuration = Configuration.getProjectInstance(host.project).advancedConfiguration
        if (!configuration.isSourceModificationAllowed) {
            // It's not allowed to modify code without explicit permission. Postpone adding @Inject or comment till it granted.
            host.putUserData(InjectLanguageAction.FIX_KEY, Processor { fixHost ->
                addInjectionInstructionInCode(language, fixHost)
            })
            return false
        }

        if (!addInjectionInstructionInCode(language, host)) {
            return false
        }

        TemporaryPlacesRegistry.getInstance(host.project).addHostWithUndo(host, InjectedLanguage.create(language.id))
        return true
    }

    override fun removeInjectionInPlace(psiElement: PsiLanguageInjectionHost?): Boolean {
        if (psiElement == null || psiElement !is KtElement) return false

        val project = psiElement.getProject()

        val injectionAnnotation = findAnnotationInjection(psiElement)
        val injectionComment = findInjectionComment(psiElement)

        val injectInstructions = listOf(injectionAnnotation, injectionComment).filterNotNull()

        val configuration = Configuration.getProjectInstance(project)

        TemporaryPlacesRegistry.getInstance(project).removeHostWithUndo(project, psiElement)
        configuration.replaceInjectionsWithUndo(project, listOf(), listOf(), injectInstructions)

        return true
    }

    override fun findCommentInjection(host: PsiElement, commentRef: Ref<PsiElement>?): BaseInjection? {
        // Do not inject through CommentLanguageInjector, because it injects as simple injection.
        // We need to behave special for interpolated strings.
        return null
    }

    fun findInjectionCommentLanguageId(host: KtElement): String? {
        return InjectorUtils.findCommentInjection(host, "", null)?.injectedLanguageId
    }

    fun findInjectionComment(host: KtElement): PsiComment? {
        val commentRef = Ref.create<PsiElement>(null)
        InjectorUtils.findCommentInjection(host, "", commentRef) ?: return null

        return commentRef.get() as? PsiComment
    }

    fun findAnnotationInjectionLanguageId(host: KtElement): String? {
        val annotationEntry = findAnnotationInjection(host) ?: return null
        return extractLanguageFromInjectAnnotation(annotationEntry)
    }
}

fun extractLanguageFromInjectAnnotation(annotationEntry: KtAnnotationEntry): String? {
    val firstArgument: ValueArgument = annotationEntry.valueArguments.firstOrNull() ?: return null

    val firstStringArgument = firstArgument.getArgumentExpression() as? KtStringTemplateExpression ?: return null
    val firstStringEntry = firstStringArgument.entries.singleOrNull() ?: return null

    return firstStringEntry.text
}

private fun findAnnotationInjection(host: KtElement): KtAnnotationEntry? {
    val modifierListOwner = findElementToInjectWithAnnotation(host) ?: return null

    val modifierList = modifierListOwner.modifierList ?: return null

    // Host can't be before annotation
    if (host.startOffset < modifierList.endOffset) return null

    return modifierListOwner.findAnnotation(FqName(AnnotationUtil.LANGUAGE))
}

private fun canInjectWithAnnotation(host: PsiElement): Boolean {
    val module = ModuleUtilCore.findModuleForPsiElement(host) ?: return false
    val javaPsiFacade = JavaPsiFacade.getInstance(module.project)

    return javaPsiFacade.findClass(AnnotationUtil.LANGUAGE, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)) != null
}

private fun findElementToInjectWithAnnotation(host: KtElement): KtModifierListOwner? {
    return PsiTreeUtil.getParentOfType(
            host,
            KtModifierListOwner::class.java,
            false, /* strict */
            KtBlockExpression::class.java, KtParameterList::class.java, KtTypeParameterList::class.java /* Stop at */
    )
}

private fun findElementToInjectWithComment(host: KtElement): KtExpression? {
    val parentBlockExpression = PsiTreeUtil.getParentOfType(
            host,
            KtBlockExpression::class.java,
            true, /* strict */
            KtDeclaration::class.java /* Stop at */
    ) ?: return null

    return parentBlockExpression.statements.firstOrNull { statement ->
        PsiTreeUtil.isAncestor(statement, host, false) && checkIsValidPlaceForInjectionWithLineComment(statement, host)
    }
}

private fun addInjectionInstructionInCode(language: Language, host: PsiLanguageInjectionHost): Boolean {
    val ktHost = host as? KtElement ?: return false
    val project = ktHost.project

    // Find the place where injection can be stated with annotation or comment
    val modifierListOwner = findElementToInjectWithAnnotation(ktHost)

    if (modifierListOwner != null && canInjectWithAnnotation(ktHost)) {
        project.executeWriteCommand("Add injection annotation") {
            modifierListOwner.addAnnotation(FqName(AnnotationUtil.LANGUAGE), "\"${language.id}\"")
        }

        return true
    }

    // Find the place where injection can be done with one-line comment
    val commentBeforeAnchor: PsiElement =
            modifierListOwner?.firstNonCommentChild() ?:
            findElementToInjectWithComment(ktHost) ?:
            return false

    val psiFactory = KtPsiFactory(project)
    val injectComment = psiFactory.createComment("//language=" + language.id)

    project.executeWriteCommand("Add injection comment") {
        commentBeforeAnchor.parent.addBefore(injectComment, commentBeforeAnchor)
    }

    return true
}

// Inspired with InjectorUtils.findCommentInjection()
private fun checkIsValidPlaceForInjectionWithLineComment(statement: KtExpression, host: KtElement): Boolean {
    // make sure comment is close enough and ...
    val statementStartOffset = statement.startOffset
    val hostStart = host.startOffset
    if (hostStart < statementStartOffset || hostStart - statementStartOffset > 120) {
        return false
    }

    if (hostStart - statementStartOffset > 2) {
        // ... there's no non-empty valid host in between comment and e2
        if (prevWalker(host, statement).asSequence().takeWhile { it != null }.any {
            it is PsiLanguageInjectionHost && it.isValidHost && !StringUtil.isEmptyOrSpaces(it.text)
        }) {
            return false
        }
    }

    return true
}

private fun PsiElement.firstNonCommentChild(): PsiElement? {
    return firstChild.siblings().dropWhile { it is PsiComment || it is PsiWhiteSpace }.firstOrNull()
}

// Based on InjectorUtils.prevWalker
private fun prevWalker(element: PsiElement, scope: PsiElement): Iterator<PsiElement?> {
    return object : Iterator<PsiElement?> {
        private var e: PsiElement? = element

        override fun hasNext(): Boolean = true
        override fun next(): PsiElement? {
            val current = e

            if (current == null || current === scope) return null
            val prev = current.prevSibling
            e = if (prev != null) {
                getDeepestLast(prev)
            }
            else {
                val parent = current.parent
                if (parent === scope || parent is PsiFile) null else parent
            }
            return e
        }
    }
}

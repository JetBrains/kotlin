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

package org.jetbrains.kotlin.idea.refactoring.rename

import org.jetbrains.kotlin.statistics.KotlinStatisticsTrigger
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.PsiElementRenameHandler
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPlain
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.statistics.KotlinIdeRefactoringTrigger

class RenameJvmNameHandler : PsiElementRenameHandler() {
    private fun getStringTemplate(dataContext: DataContext): KtStringTemplateExpression? {
        val caret = CommonDataKeys.CARET.getData(dataContext) ?: return null
        val ktFile = CommonDataKeys.PSI_FILE.getData(dataContext) as? KtFile ?: return null
        return ktFile.findElementAt(caret.offset)?.getNonStrictParentOfType<KtStringTemplateExpression>()
    }

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val nameExpression = getStringTemplate(dataContext) ?: return false
        if (!nameExpression.isPlain()) return false
        val entry = ((nameExpression.parent as? KtValueArgument)?.parent as? KtValueArgumentList)?.parent as? KtAnnotationEntry
                    ?: return false
        val annotationType = entry.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, entry.typeReference]
                             ?: return false
        return annotationType.constructor.declarationDescriptor?.importableFqName == DescriptorUtils.JVM_NAME
    }

    private fun wrapDataContext(dataContext: DataContext): DataContext? {
        val nameExpression = getStringTemplate(dataContext) ?: return null
        val name = nameExpression.plainContent
        val entry = nameExpression.getStrictParentOfType<KtAnnotationEntry>() ?: return null
        val annotationList = PsiTreeUtil.getParentOfType(entry, KtModifierList::class.java, KtFileAnnotationList::class.java)
        val newElement = when (annotationList) {
            is KtModifierList ->
                (annotationList.parent as? KtDeclaration)?.toLightMethods()?.firstOrNull { it.name == name } ?: return null

            is KtFileAnnotationList -> annotationList.getContainingKtFile().findFacadeClass() ?: return null

            else -> return null
        }
        return DataContext { id ->
            if (CommonDataKeys.PSI_ELEMENT.`is`(id)) return@DataContext newElement
            dataContext.getData(id)
        }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) {
        super.invoke(project, editor, file, wrapDataContext(dataContext) ?: return)
        KotlinStatisticsTrigger.trigger(KotlinIdeRefactoringTrigger::class.java, this::class.java.name)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        super.invoke(project, elements, wrapDataContext(dataContext) ?: return)
        KotlinStatisticsTrigger.trigger(KotlinIdeRefactoringTrigger::class.java, this::class.java.name)
    }
}

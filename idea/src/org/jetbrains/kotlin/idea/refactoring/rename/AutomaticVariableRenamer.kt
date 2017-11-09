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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiVariable
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.unquote
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

class AutomaticVariableRenamer(
        klass: PsiNamedElement, // PsiClass or JetClass
        newClassName: String,
        usages: Collection<UsageInfo>
) : AutomaticRenamer() {
    private val toUnpluralize = ArrayList<KtNamedDeclaration>()

    init {
        for (usage in usages) {
            val usageElement = usage.element ?: continue

            val parameterOrVariable = PsiTreeUtil.getParentOfType(
                    usageElement,
                    KtVariableDeclaration::class.java,
                    KtParameter::class.java
            ) as KtCallableDeclaration? ?: continue

            if (parameterOrVariable.typeReference?.isAncestor(usageElement) != true) continue

            val descriptor = try {
                parameterOrVariable.unsafeResolveToDescriptor()
            } catch(e: NoDescriptorForDeclarationException) {
                LOG.error(e)
                continue
            }

            val type = (descriptor as VariableDescriptor).type
            if (type.isCollectionLikeOf(klass)) {
                toUnpluralize.add(parameterOrVariable)
            }

            myElements.add(parameterOrVariable)
        }

        suggestAllNames(klass.name?.unquote(), newClassName.unquote())
    }

    override fun getDialogTitle() = RefactoringBundle.message("rename.variables.title")

    override fun getDialogDescription() = RefactoringBundle.message("rename.variables.with.the.following.names.to")

    override fun entityName() = RefactoringBundle.message("entity.name.variable")

    override fun nameToCanonicalName(name: String, element: PsiNamedElement): String? {
        if (element !is KtNamedDeclaration) return name

        val psiVariable = element.toLightElements().firstIsInstanceOrNull<PsiVariable>()
        val propertyName = if (psiVariable != null) {
            val codeStyleManager = JavaCodeStyleManager.getInstance(psiVariable.project)
            codeStyleManager.variableNameToPropertyName(name, codeStyleManager.getVariableKind(psiVariable))
        }
        else name

        if (element in toUnpluralize) {
            val singular = StringUtil.unpluralize(propertyName)
            if (singular != null) return singular
            toUnpluralize.remove(element)
        }
        return propertyName
    }

    override fun canonicalNameToName(canonicalName: String, element: PsiNamedElement): String? {
        if (element !is KtNamedDeclaration) return canonicalName

        val psiVariable = element.toLightElements().firstIsInstanceOrNull<PsiVariable>()
        val varName = if (psiVariable != null) {
            val codeStyleManager = JavaCodeStyleManager.getInstance(psiVariable.project)
            codeStyleManager.propertyNameToVariableName(canonicalName, codeStyleManager.getVariableKind(psiVariable))
        }
        else canonicalName

        return if (element in toUnpluralize)
            StringUtil.pluralize(varName)
        else
            varName
    }

    companion object {
        val LOG = Logger.getInstance(AutomaticVariableRenamer::class.java)
    }
}

private fun KotlinType.isCollectionLikeOf(classPsiElement: PsiNamedElement): Boolean {
    val klass = this.constructor.declarationDescriptor as? ClassDescriptor ?: return false
    if (KotlinBuiltIns.isArray(this) || DescriptorUtils.isSubclass(klass, klass.builtIns.collection)) {
        val typeArgument = this.arguments.singleOrNull()?.type ?: return false
        val typePsiElement = ((typeArgument.constructor.declarationDescriptor as? ClassDescriptor)?.source as? PsiSourceElement)?.psi
        return classPsiElement == typePsiElement || typeArgument.isCollectionLikeOf(classPsiElement)
    }
    return false
}


open class AutomaticVariableRenamerFactory: AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement) = element is KtClass

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>) =
            AutomaticVariableRenamer(element as PsiNamedElement, newName, usages)

    override fun isEnabled() = JavaRefactoringSettings.getInstance().isToRenameVariables
    override fun setEnabled(enabled: Boolean) = JavaRefactoringSettings.getInstance().setRenameVariables(enabled)

    override fun getOptionName() = RefactoringBundle.message("rename.variables")
}

class AutomaticVariableRenamerFactoryForJavaClass : AutomaticVariableRenamerFactory() {
    override fun isApplicable(element: PsiElement) = element is PsiClass

    override fun getOptionName() = null
}

class AutomaticVariableInJavaRenamerFactory: AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement) = element is KtClass && element.toLightClass() != null

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>) =
            // Using java variable renamer for java usages
            com.intellij.refactoring.rename.naming.AutomaticVariableRenamer((element as KtClass).toLightClass()!!, newName, usages)

    override fun isEnabled() = JavaRefactoringSettings.getInstance().isToRenameVariables
    override fun setEnabled(enabled: Boolean) = JavaRefactoringSettings.getInstance().setRenameVariables(enabled)

    override fun getOptionName() = null
}
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
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

public class AutomaticVariableRenamer(
        klass: PsiNamedElement, // PsiClass or JetClass
        newClassName: String,
        usages: Collection<UsageInfo>
) : AutomaticRenamer() {
    private val toUnpluralize = ArrayList<KtNamedDeclaration>()

    init {
        for (usage in usages) {
            val usageElement = usage.getElement() ?: continue

            val parameterOrVariable = PsiTreeUtil.getParentOfType(
                    usageElement,
                    javaClass<KtVariableDeclaration>(),
                    javaClass<KtParameter>()
            ) ?: continue

            if (parameterOrVariable.getTypeReference()?.isAncestor(usageElement) != true) continue

            val descriptor = try {
                parameterOrVariable.resolveToDescriptor()
            } catch(e: NoDescriptorForDeclarationException) {
                LOG.error(e)
                continue
            }

            val type = (descriptor as VariableDescriptor).getType()
            if (type.isCollectionLikeOf(klass)) {
                toUnpluralize.add(parameterOrVariable)
            }

            myElements.add(parameterOrVariable)
        }

        suggestAllNames(klass.getName(), newClassName)
    }

    override fun getDialogTitle() = RefactoringBundle.message("rename.variables.title")

    override fun getDialogDescription() = RefactoringBundle.message("rename.variables.with.the.following.names.to")

    override fun entityName() = RefactoringBundle.message("entity.name.variable")

    override fun nameToCanonicalName(name: String, element: PsiNamedElement): String? {
        if (element in toUnpluralize) {
            val singular = StringUtil.unpluralize(name)
            if (singular != null) return singular
            toUnpluralize.remove(element)
        }
        return name
    }

    override fun canonicalNameToName(canonicalName: String, element: PsiNamedElement): String? {
        return if (element in toUnpluralize)
            StringUtil.pluralize(canonicalName)
        else
            canonicalName
    }

    companion object {
        val LOG = Logger.getInstance(AutomaticVariableRenamer::class.java)
    }
}

private fun KotlinType.isCollectionLikeOf(classPsiElement: PsiNamedElement): Boolean {
    val klass = this.getConstructor().getDeclarationDescriptor() as? ClassDescriptor ?: return false
    if (KotlinBuiltIns.isArray(this) || DescriptorUtils.isSubclass(klass, klass.builtIns.getCollection())) {
        val typeArgument = this.getArguments().singleOrNull()?.getType() ?: return false
        val typePsiElement = ((typeArgument.getConstructor().getDeclarationDescriptor() as? ClassDescriptor)?.getSource() as? PsiSourceElement)?.psi
        return classPsiElement == typePsiElement || typeArgument.isCollectionLikeOf(classPsiElement)
    }
    return false
}


public class AutomaticVariableRenamerFactory: AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement) = element is KtClass || element is PsiClass

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>) =
            AutomaticVariableRenamer(element as PsiNamedElement, newName, usages)

    override fun isEnabled() = JavaRefactoringSettings.getInstance().isToRenameVariables()
    override fun setEnabled(enabled: Boolean) = JavaRefactoringSettings.getInstance().setRenameVariables(enabled)

    override fun getOptionName() = RefactoringBundle.message("rename.variables")
}

public class AutomaticVariableInJavaRenamerFactory: AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement) = element is KtClass && element.toLightClass() != null

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>) =
            // Using java variable renamer for java usages
            com.intellij.refactoring.rename.naming.AutomaticVariableRenamer((element as KtClass).toLightClass()!!, newName, usages)

    override fun isEnabled() = JavaRefactoringSettings.getInstance().isToRenameVariables()
    override fun setEnabled(enabled: Boolean) = JavaRefactoringSettings.getInstance().setRenameVariables(enabled)

    override fun getOptionName() = RefactoringBundle.message("rename.variables")
}
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

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetVariableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.JetType
import java.util.ArrayList

public class AutomaticVariableRenamer(klass: JetClass, newClassName: String, usages: Collection<UsageInfo>) : AutomaticRenamer() {
    private val toUnpluralize = ArrayList<JetNamedDeclaration>()

    init {
        val classType = (klass.resolveToDescriptor() as ClassDescriptor).getDefaultType()

        for (usage in usages) {
            val usageElement = usage.getElement() ?: continue

            val parameterOrVariable = PsiTreeUtil.getParentOfType(
                    usageElement,
                    javaClass<JetVariableDeclaration>(),
                    javaClass<JetParameter>()
            ) ?: continue

            if (parameterOrVariable.getTypeReference()?.isAncestor(usageElement) != true) continue
            val type = (parameterOrVariable.resolveToDescriptor() as VariableDescriptor).getType()
            if (type.isCollectionLikeOf(classType)) {
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
}

private fun JetType.isCollectionLikeOf(elementType: JetType): Boolean {
    val klass = this.getConstructor().getDeclarationDescriptor() as? ClassDescriptor ?: return false
    if (KotlinBuiltIns.isArray(this) || DescriptorUtils.isSubclass(klass, KotlinBuiltIns.getInstance().getCollection())) {
        val typeArgument = this.getArguments().singleOrNull()?.getType() ?: return false
        return elementType == typeArgument || typeArgument.isCollectionLikeOf(elementType)
    }
    return false
}


public class AutomaticVariableRenamerFactory: AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement) = element is JetClass

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>) =
            AutomaticVariableRenamer(element as JetClass, newName, usages)

    override fun isEnabled() = JavaRefactoringSettings.getInstance().isToRenameVariables()
    override fun setEnabled(enabled: Boolean) = JavaRefactoringSettings.getInstance().setRenameVariables(enabled)

    override fun getOptionName() = RefactoringBundle.message("rename.variables")
}
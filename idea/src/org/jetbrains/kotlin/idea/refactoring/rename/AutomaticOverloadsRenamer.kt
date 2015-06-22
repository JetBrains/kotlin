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

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelExtensionsByReceiverTypeIndex
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration

public class AutomaticOverloadsRenamer(function: JetNamedFunction, newName: String) : AutomaticRenamer() {
    init {
        myElements.addAll(function.getOverloads().filter { it != function && QuickFixUtil.canModifyElement(it) })
        suggestAllNames(function.getName(), newName)
    }

    override fun getDialogTitle() = "Rename Overloads"
    override fun getDialogDescription() = "Rename overloads to:"
    override fun entityName() = "Overload"
    override fun isSelectedByDefault(): Boolean = true
}

private fun JetNamedFunction.getOverloads(): Collection<JetNamedFunction> {
    val functionName = this.getName() ?: return emptyList()

    val parent = getParent()
    val receiverTypeReference = getReceiverTypeReference()
    when (parent) {
        is JetFile -> {
            if (receiverTypeReference == null) {
                // Non-extension top-level functions in same module
                val module = ModuleUtilCore.findModuleForPsiElement(this) ?: return emptyList()
                val searchScope = GlobalSearchScope.moduleWithDependentsScope(module)
                val fqName = getFqName() ?: return emptyList()
                return JetTopLevelFunctionFqnNameIndex.getInstance()
                        .get(fqName.asString(), getProject(), searchScope)
                        .filter { it.getName() == functionName && it.getReceiverTypeReference() == null }

            } else {
                // Members and extensions for the class

                val klass = (receiverTypeReference.getTypeElement() as? JetUserType)
                                    ?.getReferenceExpression()
                                    ?.getReference()
                                    ?.resolve()
                                    as? JetClassOrObject
                            ?: return emptyList()

                return klass.getMembersAndExtensionsForClass(functionName, this.getReceiverClass() ?: return emptyList())
            }
        }
        is JetClassBody -> {
            val klass = parent.getParent() as? JetClassOrObject ?: return emptyList()

            if (receiverTypeReference == null) {
                // Members and extensions for the class
                return klass.getMembersAndExtensionsForClass(functionName, (klass.descriptor as? ClassDescriptor) ?: return emptyList())
            }
            else {
                // Members and extensions for the class
                val receiverClass = this.getReceiverClass() ?: return emptyList()
                return klass.getFunctions(functionName).filter {
                    it.isExtensionDeclaration() && it.getReceiverClass() == receiverClass
                }
            }
        }
    }
    return emptyList()
}

private fun JetClassOrObject.getMembersAndExtensionsForClass(functionName: String, classDescriptor: ClassDescriptor): List<JetNamedFunction> {
    val functionsInClass = this.getFunctions(functionName)
            .filter { !it.isExtensionDeclaration() && !it.hasModifier(JetTokens.OVERRIDE_KEYWORD) }
    return functionsInClass + this.getTopLevelExtensionFunctions(functionName, classDescriptor)
}


private fun JetClassOrObject.getFunctions(functionName: String) =
        getDeclarations()
                .filterIsInstance<JetNamedFunction>()
                .filter { it.getName() == functionName }

private fun JetClassOrObject.getTopLevelExtensionFunctions(functionName: String, classDescriptor: ClassDescriptor): List<JetNamedFunction> {
    val className = getName() ?: return emptyList()
    val searchScope = this.getUseScope() as? GlobalSearchScope ?: return emptyList()
    return JetTopLevelExtensionsByReceiverTypeIndex.INSTANCE.get(
            JetTopLevelExtensionsByReceiverTypeIndex.buildKey(className, functionName),
            getProject(),
            searchScope
    )
            .filterIsInstance<JetNamedFunction>()
            .filter { it.getReceiverClass() == classDescriptor }
}

private fun JetNamedFunction.getReceiverClass() =
        (this.descriptor as? SimpleFunctionDescriptor)
                ?.getExtensionReceiverParameter()
                ?.getType()
                ?.getConstructor()
                ?.getDeclarationDescriptor() as? ClassDescriptor

public class AutomaticOverloadsRenamerFactory : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement): Boolean {
        return element is JetNamedFunction && element.getName() != null
               && (element.getParent() is JetFile || element.getParent() is JetClassBody)
    }

    override fun getOptionName() = RefactoringBundle.message("rename.overloads")
    override fun isEnabled() = JavaRefactoringSettings.getInstance().isRenameOverloads()
    override fun setEnabled(enabled: Boolean) = JavaRefactoringSettings.getInstance().setRenameOverloads(enabled)
    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>)
            = AutomaticOverloadsRenamer(element as JetNamedFunction, newName)
}
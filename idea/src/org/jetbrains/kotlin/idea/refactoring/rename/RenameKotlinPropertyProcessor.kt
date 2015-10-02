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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.SyntheticElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.util.RefactoringUtil
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.OverrideResolver

public class RenameKotlinPropertyProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        val namedUnwrappedElement = element.namedUnwrappedElement
        return namedUnwrappedElement is JetProperty || (namedUnwrappedElement is JetParameter && namedUnwrappedElement.hasValOrVar())
    }

    /* Can't properly update getters and setters in Java */
    override fun isInplaceRenameSupported() = false

    override fun substituteElementToRename(element: PsiElement?, editor: Editor?): PsiElement? {
        val namedUnwrappedElement = element?.namedUnwrappedElement

        val callableDeclaration = namedUnwrappedElement as? JetCallableDeclaration
        if (callableDeclaration == null) throw IllegalStateException("Can't be for element $element there because of canProcessElement()")

        val deepestSuperDeclaration = findDeepestOverriddenDeclaration(callableDeclaration)
        if (deepestSuperDeclaration == null || deepestSuperDeclaration == callableDeclaration) {
            return callableDeclaration
        }

        if (ApplicationManager.getApplication()!!.isUnitTestMode()) {
            return deepestSuperDeclaration
        }

        val containsText: String? =
                deepestSuperDeclaration.getFqName()?.parent()?.asString() ?:
                (deepestSuperDeclaration.getParent() as? JetClassOrObject)?.getName()

        val result = Messages.showYesNoCancelDialog(
                deepestSuperDeclaration.getProject(),
                if (containsText != null) "Do you want to rename base property from \n$containsText" else "Do you want to rename base property",
                "Rename warning",
                Messages.getQuestionIcon())

        return when (result) {
            Messages.YES -> deepestSuperDeclaration
            Messages.NO -> callableDeclaration
            else -> /* Cancel rename */ null
        }
    }

    override fun prepareRenaming(element: PsiElement?, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        val namedUnwrappedElement = element?.namedUnwrappedElement
        val propertyMethods = when(namedUnwrappedElement) {
            is JetProperty -> runReadAction { LightClassUtil.getLightClassPropertyMethods(namedUnwrappedElement) }
            is JetParameter -> runReadAction { LightClassUtil.getLightClassPropertyMethods(namedUnwrappedElement) }
            else -> throw IllegalStateException("Can't be for element $element there because of canProcessElement()")
        }

        for (propertyMethod in propertyMethods) {
            addRenameElements(propertyMethod, (element as PsiNamedElement).name, newName, allRenames, scope)
        }
    }

    private enum class UsageKind {
        SIMPLE_PROPERTY_USAGE,
        GETTER_USAGE,
        SETTER_USAGE
    }

    override fun renameElement(element: PsiElement?, newName: String?, usages: Array<out UsageInfo>, listener: RefactoringElementListener?) {
        if (element !is JetProperty && element !is JetParameter) {
            super.renameElement(element, newName, usages, listener)
            return
        }

        val name = (element as JetNamedDeclaration).getName()!!
        val oldGetterName = JvmAbi.getterName(name)
        val oldSetterName = JvmAbi.setterName(name)

        val refKindUsages = usages.toList().groupBy { usage: UsageInfo ->
            val refElement = usage.getReference()?.resolve()
            if (refElement is PsiMethod) {
                when (refElement.getName()) {
                    oldGetterName -> UsageKind.GETTER_USAGE
                    oldSetterName -> UsageKind.SETTER_USAGE
                    else -> UsageKind.SIMPLE_PROPERTY_USAGE
                }
            }
            else {
                UsageKind.SIMPLE_PROPERTY_USAGE
            }
        }

        super.renameElement(element, JvmAbi.setterName(newName!!),
                            refKindUsages[UsageKind.SETTER_USAGE]?.toTypedArray() ?: arrayOf<UsageInfo>(),
                            null)

        super.renameElement(element, JvmAbi.getterName(newName),
                            refKindUsages[UsageKind.GETTER_USAGE]?.toTypedArray() ?: arrayOf<UsageInfo>(),
                            null)

        super.renameElement(element, newName,
                            refKindUsages[UsageKind.SIMPLE_PROPERTY_USAGE]?.toTypedArray() ?: arrayOf<UsageInfo>(),
                            null)

        dropOverrideKeywordIfNecessary(element)

        listener?.elementRenamed(element)
    }

    private fun addRenameElements(psiMethod: PsiMethod?,
                                  oldName: String?, newName: String?,
                                  allRenames: MutableMap<PsiElement, String>,
                                  scope: SearchScope) {
        if (psiMethod == null) return

        OverridingMethodsSearch.search(psiMethod, scope, true).forEach { overrider ->
            val overriderElement = overrider.namedUnwrappedElement

            if (overriderElement != null && overriderElement !is SyntheticElement) {
                RenameProcessor.assertNonCompileElement(overriderElement)

                val overriderName = overriderElement.getName()

                if (overriderElement is PsiMethod) {
                    if (newName != null && Name.isValidIdentifier(newName)) {
                        val isGetter = overriderElement.getParameterList().getParametersCount() == 0
                        allRenames[overriderElement] = if (isGetter) JvmAbi.getterName(newName) else JvmAbi.setterName(newName)
                    }
                }
                else {
                    val newOverriderName = RefactoringUtil.suggestNewOverriderName(overriderName, oldName, newName)
                    if (newOverriderName != null) {
                        allRenames[overriderElement] = newOverriderName
                    }
                }
            }
        }
    }

    private fun findDeepestOverriddenDeclaration(declaration: JetCallableDeclaration): JetCallableDeclaration? {
        if (declaration.getModifierList()?.hasModifier(JetTokens.OVERRIDE_KEYWORD) == true) {
            val bindingContext = declaration.analyze()
            var descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
            if (descriptor is ValueParameterDescriptor) {
                descriptor = bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, descriptor]
                    ?: return declaration
            }

            if (descriptor != null) {
                assert(descriptor is PropertyDescriptor, "Property descriptor is expected")

                val supers = OverrideResolver.getDeepestSuperDeclarations(descriptor as PropertyDescriptor)

                // Take one of supers for now - API doesn't support substitute to several elements (IDEA-48796)
                val deepest = supers.first()
                if (deepest != descriptor) {
                    val superPsiElement = DescriptorToSourceUtils.descriptorToDeclaration(deepest)
                    return superPsiElement as? JetCallableDeclaration
                }
            }
        }

        return null
    }
}

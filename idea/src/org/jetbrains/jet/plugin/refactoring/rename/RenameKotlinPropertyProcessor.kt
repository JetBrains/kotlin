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

package org.jetbrains.jet.plugin.refactoring.rename

import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetProperty
import com.intellij.psi.search.SearchScope
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.jet.asJava.LightClassUtil
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.openapi.util.Computable
import org.jetbrains.jet.asJava.LightClassUtil.PropertyAccessorsPsiMethods
import com.intellij.psi.PsiMethod
import com.intellij.psi.SyntheticElement
import com.intellij.refactoring.util.RefactoringUtil
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.jet.codegen.PropertyCodegen
import org.jetbrains.jet.lang.resolve.name.Name
import com.intellij.usageView.UsageInfo
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetClassOrObject
import com.intellij.openapi.ui.Messages
import org.jetbrains.jet.lang.resolve.OverridingUtil
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.asJava.namedUnwrappedElement
import org.jetbrains.jet.lang.resolve.OverrideResolver
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils

public class RenameKotlinPropertyProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean = element.namedUnwrappedElement is JetProperty

    /* Can't properly update getters and setters in Java */
    override fun isInplaceRenameSupported() = false

    override fun substituteElementToRename(element: PsiElement?, editor: Editor?): PsiElement? {
        val jetProperty = element?.namedUnwrappedElement as? JetProperty
        if (jetProperty == null) throw IllegalStateException("Can't be for element $element there because of canProcessElement()")

        val deepestSuperProperty = findDeepestOverriddenProperty(jetProperty)
        if (deepestSuperProperty == null || deepestSuperProperty == jetProperty) {
            return jetProperty
        }

        if (ApplicationManager.getApplication()!!.isUnitTestMode()) {
            return deepestSuperProperty
        }

        val containsText: String? =
                deepestSuperProperty.getFqName()?.parent()?.asString() ?:
                (deepestSuperProperty.getParent() as? JetClassOrObject)?.getName()

        val result = Messages.showYesNoCancelDialog(
                deepestSuperProperty.getProject(),
                if (containsText != null) "Do you want to rename base property from \n$containsText" else "Do you want to rename base property",
                "Rename warning",
                Messages.getQuestionIcon())

        return when (result) {
            Messages.YES -> deepestSuperProperty
            Messages.NO -> jetProperty
            else -> /* Cancel rename */ null
        }
    }

    override fun prepareRenaming(element: PsiElement?, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        val jetProperty = element?.namedUnwrappedElement as? JetProperty
        if (jetProperty == null) throw IllegalStateException("Can't be for element $element there because of canProcessElement()")

        val propertyMethods = ApplicationManager.getApplication()!!.runReadAction(Computable<PropertyAccessorsPsiMethods> {
            LightClassUtil.getLightClassPropertyMethods(jetProperty)
        })!!

        for (propertyMethod in propertyMethods) {
            addRenameElements(propertyMethod, jetProperty.getName(), newName, allRenames, scope)
        }
    }

    override fun renameElement(element: PsiElement?, newName: String?, usages: Array<out UsageInfo>, listener: RefactoringElementListener?) {
        if (element !is JetProperty) {
            super.renameElement(element, newName, usages, listener)
            return
        }

        enum class UsageKind {
            SIMPLE_PROPERTY_USAGE
            GETTER_USAGE
            SETTER_USAGE
        }

        val oldGetterName = PropertyCodegen.getterName(element.getNameAsName())
        val oldSetterName = PropertyCodegen.setterName(element.getNameAsName())

        val refKindUsages = usages.toList().groupBy { (usage: UsageInfo): UsageKind ->
            val refElement = usage.getReference()!!.resolve()
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

        super.renameElement(element, PropertyCodegen.setterName(Name.identifier(newName!!)),
                            refKindUsages[UsageKind.SETTER_USAGE]?.copyToArray() ?: array<UsageInfo>(),
                            null)

        super.renameElement(element, PropertyCodegen.getterName(Name.identifier(newName)),
                            refKindUsages[UsageKind.GETTER_USAGE]?.copyToArray() ?: array<UsageInfo>(),
                            null)

        super.renameElement(element, newName,
                            refKindUsages[UsageKind.SIMPLE_PROPERTY_USAGE]?.copyToArray() ?: array<UsageInfo>(),
                            null)

        listener?.elementRenamed(element)
    }

    private fun addRenameElements(psiMethod: PsiMethod?,
                                  oldName: String?, newName: String?,
                                  allRenames: MutableMap<PsiElement, String>,
                                  scope: SearchScope) {
        if (psiMethod == null) return

        OverridingMethodsSearch.search(psiMethod, scope, true).forEach { overrider ->
            val overriderElement = overrider.namedUnwrappedElement

            if (overriderElement != null && !(overriderElement is SyntheticElement)) {
                RenameProcessor.assertNonCompileElement(overriderElement)

                val overriderName = overriderElement.getName()

                if (overriderElement is PsiMethod) {
                    if (newName != null && Name.isValidIdentifier(newName)) {
                        val isGetter = overriderElement.getParameterList().getParametersCount() == 0
                        val name = Name.identifier(newName)

                        allRenames[overriderElement] = if (isGetter) PropertyCodegen.getterName(name) else PropertyCodegen.setterName(name)
                    }
                }
                else {
                    val newOverriderName = RefactoringUtil.suggestNewOverriderName(overriderName, oldName, newName)
                    if (newOverriderName != null) {
                        allRenames[overriderElement] = newOverriderName
                    }
                }
            }

            true
        }
    }

    private fun findDeepestOverriddenProperty(jetProperty: JetProperty): JetProperty? {
        if (jetProperty.getModifierList()?.hasModifier(JetTokens.OVERRIDE_KEYWORD) == true) {
            val bindingContext = AnalyzerFacadeWithCache.getContextForElement(jetProperty)
            val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, jetProperty]

            if (descriptor != null) {
                assert(descriptor is PropertyDescriptor, "Property descriptor is expected")

                val supers = OverrideResolver.getDeepestSuperDeclarations(descriptor as PropertyDescriptor)

                // Take one of supers for now - API doesn't support substitute to several elements (IDEA-48796)
                val deepest = supers.first()
                if (deepest != descriptor) {
                    val superPsiElement = DescriptorToSourceUtils.descriptorToDeclaration(deepest)
                    return superPsiElement as? JetProperty
                }
            }
        }

        return null
    }
}
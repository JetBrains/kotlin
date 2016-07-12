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
import com.intellij.psi.*
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.util.RefactoringUtil
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.dropOverrideKeywordIfNecessary
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverrideResolver

class RenameKotlinPropertyProcessor : RenameKotlinPsiProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        val namedUnwrappedElement = element.namedUnwrappedElement
        return namedUnwrappedElement is KtProperty || (namedUnwrappedElement is KtParameter && namedUnwrappedElement.hasValOrVar())
    }

    /* Can't properly update getters and setters in Java */
    override fun isInplaceRenameSupported() = false

    private fun getJvmNames(element: PsiElement): Pair<String?, String?> {
        val descriptor = (element.unwrapped as? KtDeclaration)?.resolveToDescriptor() as? PropertyDescriptor ?: return null to null
        val getterName = descriptor.getter?.let { DescriptorUtils.getJvmName(it) }
        val setterName = descriptor.setter?.let { DescriptorUtils.getJvmName(it) }
        return getterName to setterName
    }

    override fun findReferences(element: PsiElement): Collection<PsiReference> {
        val allReferences = super.findReferences(element)
        val (getterJvmName, setterJvmName) = getJvmNames(element)
        return when {
            getterJvmName == null && setterJvmName == null -> allReferences
            element is KtElement -> allReferences.filter {
                it is KtReference
                || (getterJvmName == null && (it.resolve() as? PsiNamedElement)?.name != setterJvmName)
                || (setterJvmName == null && (it.resolve() as? PsiNamedElement)?.name != getterJvmName)
            }
            element is KtLightElement<*, *> -> {
                val name = element.name
                if (name == getterJvmName || name == setterJvmName) allReferences.filterNot { it is KtReference } else allReferences
            }
            else -> emptyList()
        }
    }

    private fun chooseCallableToRename(callableDeclaration: KtCallableDeclaration): KtCallableDeclaration? {
        val deepestSuperDeclaration = findDeepestOverriddenDeclaration(callableDeclaration)
        if (deepestSuperDeclaration == null || deepestSuperDeclaration == callableDeclaration) {
            return callableDeclaration
        }

        if (ApplicationManager.getApplication()!!.isUnitTestMode) return deepestSuperDeclaration

        val containsText: String? =
                deepestSuperDeclaration.fqName?.parent()?.asString() ?:
                (deepestSuperDeclaration.parent as? KtClassOrObject)?.name

        val result = Messages.showYesNoCancelDialog(
                deepestSuperDeclaration.project,
                if (containsText != null) "Do you want to rename base property from \n$containsText" else "Do you want to rename base property",
                "Rename warning",
                Messages.getQuestionIcon())

        return when (result) {
            Messages.YES -> deepestSuperDeclaration
            Messages.NO -> callableDeclaration
            else -> /* Cancel rename */ null
        }
    }

    override fun substituteElementToRename(element: PsiElement?, editor: Editor?): PsiElement? {
        val namedUnwrappedElement = element?.namedUnwrappedElement ?: return null

        val callableDeclaration = namedUnwrappedElement as? KtCallableDeclaration
                                  ?: throw IllegalStateException("Can't be for element $element there because of canProcessElement()")

        val declarationToRename = chooseCallableToRename(callableDeclaration) ?: return null

        val (getterJvmName, setterJvmName) = getJvmNames(namedUnwrappedElement)
        if (element is KtLightMethod) {
            val name = element.name
            if (element.name != getterJvmName && element.name != setterJvmName) return declarationToRename
            return declarationToRename.toLightMethods().firstOrNull { it.name == name }
        }

        return declarationToRename
    }

    override fun prepareRenaming(element: PsiElement, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        super.prepareRenaming(element, newName, allRenames, scope)

        val namedUnwrappedElement = element.namedUnwrappedElement
        val propertyMethods = when(namedUnwrappedElement) {
            is KtProperty -> runReadAction { LightClassUtil.getLightClassPropertyMethods(namedUnwrappedElement) }
            is KtParameter -> runReadAction { LightClassUtil.getLightClassPropertyMethods(namedUnwrappedElement) }
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

    override tailrec fun renameElement(element: PsiElement, newName: String, usages: Array<out UsageInfo>, listener: RefactoringElementListener?) {
        if (element is KtLightMethod) {
            if (element.modifierList.findAnnotation(DescriptorUtils.JVM_NAME.asString()) != null) {
                return super.renameElement(element, newName, usages, listener)
            }

            val origin = element.kotlinOrigin
            val newPropertyName = propertyNameByAccessor(newName, element)
            // Kotlin references to Kotlin property should not use accessor name
            if (newPropertyName != null && (origin is KtProperty || origin is KtParameter)) {
                val (ktUsages, otherUsages) = usages.partition { it.reference is KtSimpleNameReference }
                super.renameElement(element, newName, otherUsages.toTypedArray(), listener)
                renameElement(origin, newPropertyName, ktUsages.toTypedArray(), listener)
                return
            }
        }

        if (element !is KtProperty && element !is KtParameter) {
            super.renameElement(element, newName, usages, listener)
            return
        }

        val name = (element as KtNamedDeclaration).name!!
        val oldGetterName = JvmAbi.getterName(name)
        val oldSetterName = JvmAbi.setterName(name)

        val refKindUsages = usages.toList().groupBy { usage: UsageInfo ->
            val refElement = usage.reference?.resolve()
            if (refElement is PsiMethod) {
                when (refElement.name) {
                    oldGetterName -> UsageKind.GETTER_USAGE
                    oldSetterName -> UsageKind.SETTER_USAGE
                    else -> UsageKind.SIMPLE_PROPERTY_USAGE
                }
            }
            else {
                UsageKind.SIMPLE_PROPERTY_USAGE
            }
        }

        super.renameElement(element, JvmAbi.setterName(newName),
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
                                  oldName: String?,
                                  newName: String?,
                                  allRenames: MutableMap<PsiElement, String>,
                                  scope: SearchScope) {
        if (psiMethod == null) return

        OverridingMethodsSearch.search(psiMethod, scope, true).forEach { overrider ->
            val overriderElement = overrider.namedUnwrappedElement

            if (overriderElement != null && overriderElement !is SyntheticElement) {
                RenameProcessor.assertNonCompileElement(overriderElement)

                val overriderName = overriderElement.name

                if (overriderElement is PsiMethod) {
                    if (newName != null && Name.isValidIdentifier(newName)) {
                        val isGetter = overriderElement.parameterList.parametersCount == 0
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
            true
        }
    }

    private fun findDeepestOverriddenDeclaration(declaration: KtCallableDeclaration): KtCallableDeclaration? {
        if (declaration.modifierList?.hasModifier(KtTokens.OVERRIDE_KEYWORD) == true) {
            val bindingContext = declaration.analyze()
            var descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
            if (descriptor is ValueParameterDescriptor) {
                descriptor = bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, descriptor]
                    ?: return declaration
            }

            if (descriptor != null) {
                assert(descriptor is PropertyDescriptor) { "Property descriptor is expected" }

                val supers = OverrideResolver.getDeepestSuperDeclarations(descriptor as PropertyDescriptor)

                // Take one of supers for now - API doesn't support substitute to several elements (IDEA-48796)
                val deepest = supers.first()
                if (deepest != descriptor) {
                    val superPsiElement = DescriptorToSourceUtils.descriptorToDeclaration(deepest)
                    return superPsiElement as? KtCallableDeclaration
                }
            }
        }

        return null
    }
}

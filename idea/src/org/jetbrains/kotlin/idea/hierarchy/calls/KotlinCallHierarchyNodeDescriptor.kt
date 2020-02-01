/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.hierarchy.calls

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.roots.ui.util.CompositeAppearance
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.ui.LayeredIcon
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.awt.Font
import java.util.*

class KotlinCallHierarchyNodeDescriptor(
    parentDescriptor: HierarchyNodeDescriptor?,
    element: KtElement,
    isBase: Boolean,
    navigateToReference: Boolean
) : HierarchyNodeDescriptor(element.project, parentDescriptor, element, isBase),
    Navigatable {
    private var usageCount = 1
    private val references: MutableSet<PsiReference> = HashSet()
    private val javaDelegate: CallHierarchyNodeDescriptor

    fun incrementUsageCount() {
        usageCount++
        javaDelegate.incrementUsageCount()
    }

    fun addReference(reference: PsiReference) {
        references.add(reference)
        javaDelegate.addReference(reference)
    }

    override fun isValid(): Boolean {
        val myElement = psiElement
        return myElement != null && myElement.isValid
    }

    override fun update(): Boolean {
        val oldText = myHighlightedText
        val oldIcon = icon

        val flags: Int = if (isMarkReadOnly) {
            Iconable.ICON_FLAG_VISIBILITY or Iconable.ICON_FLAG_READ_STATUS
        } else {
            Iconable.ICON_FLAG_VISIBILITY
        }

        var changes = super.update()

        val elementText = renderElement(psiElement)
        if (elementText == null) {
            val invalidPrefix = IdeBundle.message("node.hierarchy.invalid")
            if (!myHighlightedText.text.startsWith(invalidPrefix)) {
                myHighlightedText.beginning.addText(invalidPrefix, getInvalidPrefixAttributes())
            }
            return true
        }

        val targetElement = psiElement
        val elementIcon = targetElement!!.getIcon(flags)

        icon = if (changes && myIsBase) {
            LayeredIcon(2).apply {
                setIcon(elementIcon, 0)
                setIcon(AllIcons.General.Modified, 1, -AllIcons.General.Modified.iconWidth / 2, 0)
            }
        } else {
            elementIcon
        }

        val mainTextAttributes: TextAttributes? = if (myColor != null) {
            TextAttributes(myColor, null, null, null, Font.PLAIN)
        } else {
            null
        }

        myHighlightedText = CompositeAppearance()
        myHighlightedText.ending.addText(elementText, mainTextAttributes)
        if (usageCount > 1) {
            myHighlightedText.ending.addText(
                IdeBundle.message("node.call.hierarchy.N.usages", usageCount),
                getUsageCountPrefixAttributes(),
            )
        }

        val packageName = KtPsiUtil.getPackageName(targetElement as KtElement) ?: ""

        myHighlightedText.ending.addText("  ($packageName)", getPackageNameAttributes())
        myName = myHighlightedText.text

        if (!(Comparing.equal(myHighlightedText, oldText) && Comparing.equal(icon, oldIcon))) {
            changes = true
        }

        return changes
    }

    override fun navigate(requestFocus: Boolean) {
        javaDelegate.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return javaDelegate.canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
        return javaDelegate.canNavigateToSource()
    }

    companion object {
        private fun renderElement(element: PsiElement?): String? {
            when (element) {
                is KtFile -> {
                    return element.name
                }
                !is KtNamedDeclaration -> {
                    return null
                }
                else -> {
                    var descriptor: DeclarationDescriptor = element.resolveToDescriptorIfAny(BodyResolveMode.PARTIAL) ?: return null
                    val elementText: String?
                    when (element) {
                        is KtClassOrObject -> {
                            when {
                                element is KtObjectDeclaration && element.isCompanion() -> {
                                    val containingDescriptor = descriptor.containingDeclaration
                                    if (containingDescriptor !is ClassDescriptor) return null
                                    descriptor = containingDescriptor
                                    elementText = renderClassOrObject(descriptor)
                                }
                                element is KtEnumEntry -> {
                                    elementText = element.name
                                }
                                else -> {
                                    elementText = if (element.name != null) {
                                        renderClassOrObject(descriptor as ClassDescriptor)
                                    } else {
                                        "[anonymous]"
                                    }
                                }
                            }
                        }
                        is KtNamedFunction, is KtConstructor<*> -> {
                            if (descriptor !is FunctionDescriptor) return null
                            elementText = renderNamedFunction(descriptor)
                        }
                        is KtProperty -> {
                            elementText = element.name
                        }
                        else -> return null
                    }

                    if (elementText == null) return null
                    var containerText: String? = null
                    var containerDescriptor = descriptor.containingDeclaration
                    while (containerDescriptor != null) {
                        if (containerDescriptor is PackageFragmentDescriptor || containerDescriptor is ModuleDescriptor) {
                            break
                        }
                        val name = containerDescriptor.name
                        if (!name.isSpecial) {
                            val identifier = name.identifier
                            containerText = if (containerText != null) "$identifier.$containerText" else identifier
                        }
                        containerDescriptor = containerDescriptor.containingDeclaration
                    }
                    return if (containerText != null) "$containerText.$elementText" else elementText
                }
            }
        }

        fun renderNamedFunction(descriptor: FunctionDescriptor): String {
            val descriptorForName: DeclarationDescriptor =
                (descriptor as? ConstructorDescriptor)?.containingDeclaration ?: descriptor
            val name = descriptorForName.name.asString()
            val paramTypes =
                StringUtil.join(
                    descriptor.valueParameters,
                    { descriptor1: ValueParameterDescriptor ->
                        DescriptorRenderer
                            .SHORT_NAMES_IN_TYPES
                            .renderType(descriptor1.type)
                    },
                    ", "
                )
            return "$name($paramTypes)"
        }

        private fun renderClassOrObject(descriptor: ClassDescriptor): String {
            return descriptor.name.asString()
        }
    }

    init {
        javaDelegate = CallHierarchyNodeDescriptor(myProject, null, element, isBase, navigateToReference)
    }
}
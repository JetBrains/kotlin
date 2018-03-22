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

package org.jetbrains.kotlin.idea.hierarchy.overrides

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.roots.ui.util.CompositeAppearance
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.ui.LayeredIcon
import com.intellij.ui.RowIcon
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import java.awt.Font
import javax.swing.Icon

class KotlinOverrideHierarchyNodeDescriptor (
        parentNode: HierarchyNodeDescriptor?,
        klass: PsiElement,
        baseElement: PsiElement
) : HierarchyNodeDescriptor(klass.project, parentNode, klass, parentNode == null) {
    private val baseElement = baseElement.createSmartPointer()

    private var rawIcon: Icon? = null
    private var stateIcon: Icon? = null

    private fun resolveToDescriptor(psiElement: PsiElement): DeclarationDescriptor? {
        return when (psiElement) {
            is KtNamedDeclaration -> psiElement.unsafeResolveToDescriptor()
            is PsiMember -> psiElement.getJavaMemberDescriptor()
            else -> null
        }
    }

    private fun getBaseDescriptor() = baseElement.element?.let { resolveToDescriptor(it) } as? CallableMemberDescriptor

    private fun getCurrentClassDescriptor() = psiElement?.let { resolveToDescriptor(it) } as? ClassDescriptor

    private fun getCurrentDescriptor(): CallableMemberDescriptor? {
        val classDescriptor = getCurrentClassDescriptor() ?: return null
        val baseDescriptor = getBaseDescriptor() ?: return null
        val baseClassDescriptor = baseDescriptor.containingDeclaration as? ClassDescriptor ?: return null
        val substitutor = getTypeSubstitutor(baseClassDescriptor.defaultType, classDescriptor.defaultType) ?: return null
        return classDescriptor.findCallableMemberBySignature(baseDescriptor.substitute(substitutor) as CallableMemberDescriptor)
    }

    internal fun calculateState(): Icon? {
        val classDescriptor = getCurrentClassDescriptor() ?: return null
        val callableDescriptor = getCurrentDescriptor() ?: return AllIcons.Hierarchy.MethodNotDefined

        if (callableDescriptor.kind == CallableMemberDescriptor.Kind.DECLARATION) {
            if (callableDescriptor.modality == Modality.ABSTRACT) return null
            return AllIcons.Hierarchy.MethodDefined
        }

        val isAbstractClass = classDescriptor.modality == Modality.ABSTRACT
        val hasBaseImplementation = DescriptorUtils.getAllOverriddenDeclarations(callableDescriptor).any { it.modality != Modality.ABSTRACT }
        return if (isAbstractClass || hasBaseImplementation) AllIcons.Hierarchy.MethodNotDefined else AllIcons.Hierarchy.ShouldDefineMethod
    }

    override fun update(): Boolean {
        var flags = Iconable.ICON_FLAG_VISIBILITY
        if (isMarkReadOnly) {
            flags = flags or Iconable.ICON_FLAG_READ_STATUS
        }

        var changes = super.update()

        val classPsi = psiElement
        val classDescriptor = getCurrentClassDescriptor()
        if (classPsi == null || classDescriptor == null) {
            val invalidPrefix = IdeBundle.message("node.hierarchy.invalid")
            if (!myHighlightedText.text.startsWith(invalidPrefix)) {
                myHighlightedText.beginning.addText(invalidPrefix, getInvalidPrefixAttributes())
            }
            return true
        }

        val newRawIcon = classPsi.getIcon(flags)
        val newStateIcon = calculateState()

        if (changes || newRawIcon !== rawIcon || newStateIcon !== stateIcon) {
            changes = true

            rawIcon = newRawIcon
            stateIcon = newStateIcon

            var newIcon = rawIcon

            if (myIsBase) {
                val icon = LayeredIcon(2)
                icon.setIcon(newIcon, 0)
                icon.setIcon(AllIcons.Hierarchy.Base, 1, -AllIcons.Hierarchy.Base.iconWidth / 2, 0)
                newIcon = icon
            }

            if (stateIcon != null) {
                newIcon = RowIcon(stateIcon, newIcon)
            }

            icon = newIcon
        }

        val oldText = myHighlightedText

        myHighlightedText = CompositeAppearance()
        var classNameAttributes: TextAttributes? = null
        if (myColor != null) {
            classNameAttributes = TextAttributes(myColor, null, null, null, Font.PLAIN)
        }

        with (myHighlightedText.ending) {
            addText(classDescriptor.name.asString(), classNameAttributes)
            classDescriptor.parents.forEach { parentDescriptor ->
                when (parentDescriptor) {
                    is MemberDescriptor -> {
                        addText(" in ${parentDescriptor.name.asString()}", classNameAttributes)
                        if (parentDescriptor is FunctionDescriptor) {
                            addText("()", classNameAttributes)
                        }
                    }
                    is PackageFragmentDescriptor -> {
                        addText("  (${parentDescriptor.fqName.asString()})", HierarchyNodeDescriptor.getPackageNameAttributes())
                        return@forEach
                    }
                }
            }
        }

        myName = myHighlightedText.text

        if (!Comparing.equal(myHighlightedText, oldText)) {
            changes = true
        }

        return changes
    }
}
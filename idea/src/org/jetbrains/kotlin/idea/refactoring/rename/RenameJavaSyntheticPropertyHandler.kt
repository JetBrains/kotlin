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

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

class RenameJavaSyntheticPropertyHandler : AbstractReferenceSubstitutionRenameHandler<SyntheticJavaPropertyDescriptor>() {
    class Processor : RenamePsiElementProcessor() {
        override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
            val propertyWrapper = element as? SyntheticPropertyWrapper ?: return
            propertyWrapper.getter?.let { allRenames[it] = JvmAbi.getterName(newName) }
            propertyWrapper.setter?.let { allRenames[it] = JvmAbi.setterName(newName) }
        }

        override fun canProcessElement(element: PsiElement) = element is SyntheticPropertyWrapper
    }

    internal class SyntheticPropertyWrapper(
            manager: PsiManager,
            val descriptor: SyntheticJavaPropertyDescriptor
    ): LightElement(manager, KotlinLanguage.INSTANCE), PsiNamedElement {
        val getter: PsiMethod? get() = descriptor.getMethod.source.getPsi() as? PsiMethod
        val setter: PsiMethod? get() = descriptor.setMethod?.source?.getPsi() as? PsiMethod

        override fun getContainingFile() = getter?.containingFile

        override fun getName() = descriptor.name.asString()

        override fun setName(name: String): PsiElement? {
            getter?.name = JvmAbi.getterName(name)
            setter?.name = JvmAbi.setterName(name)
            return this
        }

        override fun toString(): String {
            val renderer = IdeDescriptorRenderers.SOURCE_CODE
            return "${renderer.render(descriptor.getMethod)}|${descriptor.setMethod?.let { renderer.render(it) }}"
        }
    }

    override fun getTargetDescriptor(dataContext: DataContext): SyntheticJavaPropertyDescriptor? {
        val refExpr = getReferenceExpression(dataContext) ?: return null
        return refExpr.analyze(BodyResolveMode.PARTIAL)[BindingContext.REFERENCE_TARGET, refExpr] as? SyntheticJavaPropertyDescriptor
    }

    override fun getElementToRename(project: Project, descriptor: SyntheticJavaPropertyDescriptor): PsiElement? {
        return SyntheticPropertyWrapper(PsiManager.getInstance(project), descriptor)
    }
}

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

package org.jetbrains.jet.plugin.hierarchy.overrides

import com.intellij.ide.hierarchy.MethodHierarchyBrowserBase
import com.intellij.psi.PsiElement
import javax.swing.JPanel
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.openapi.project.Project
import org.jetbrains.jet.plugin.hierarchy.HierarchyUtils
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.asJava.getRepresentativeLightMethod
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import org.jetbrains.jet.lang.psi.JetDeclaration
import java.util.Comparator
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import javax.swing.JTree
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.ide.hierarchy.method.MethodHierarchyBrowser
import com.intellij.ide.hierarchy.method.getComparatorByExtension
import com.intellij.ide.hierarchy.method.createTreesByExtension
import com.intellij.ide.hierarchy.method.getElementFromDescriptorByExtension

class KotlinOverrideHierarchyBrowser(
        project: Project, baseElement: PsiElement
) : MethodHierarchyBrowserBase(project, baseElement.getRepresentativeLightMethod()) {

    val javaMethodHierarchyBrowser = MethodHierarchyBrowser(project, baseElement.getRepresentativeLightMethod())

    override fun getElementFromDescriptor(descriptor: HierarchyNodeDescriptor): PsiElement? =
            javaMethodHierarchyBrowser.getElementFromDescriptorByExtension(descriptor)

    override fun createTrees(trees: MutableMap<String, JTree>) = javaMethodHierarchyBrowser.createTreesByExtension(trees)

    override fun getComparator(): Comparator<NodeDescriptor<out Any?>>? = javaMethodHierarchyBrowser.getComparatorByExtension()

    override fun createLegendPanel(): JPanel? =
            MethodHierarchyBrowserBase.createStandardLegendPanel(
                    JetBundle.message("hierarchy.legend.member.is.defined.in.class"),
                    JetBundle.message("hierarchy.legend.member.defined.in.superclass"),
                    JetBundle.message("hierarchy.legend.member.should.be.defined")
            )

    override fun isApplicableElement(element: PsiElement): Boolean =
            HierarchyUtils.IS_OVERRIDE_HIERARCHY_ELEMENT(element)

    [suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")]
    override fun createHierarchyTreeStructure(typeName: String, psiElement: PsiElement): HierarchyTreeStructure? =
            if (typeName == MethodHierarchyBrowserBase.METHOD_TYPE) KotlinOverrideTreeStructure(myProject, psiElement) else null

    override fun getContentDisplayName(typeName: String, element: PsiElement): String? {
        val targetElement = element.getNavigationElement()
        if (targetElement is JetDeclaration) {
            return ElementDescriptionUtil.getElementDescription(targetElement, RefactoringDescriptionLocation.WITHOUT_PARENT)
        }
        return super.getContentDisplayName(typeName, element)
    }
}

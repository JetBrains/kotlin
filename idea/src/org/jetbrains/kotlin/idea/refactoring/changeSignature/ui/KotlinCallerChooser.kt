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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.ui

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.changeSignature.CallerChooserBase
import com.intellij.refactoring.changeSignature.MethodNodeBase
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.Consumer
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.getJavaMethodDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.hierarchy.calls.CalleeReferenceProcessor
import org.jetbrains.kotlin.idea.hierarchy.calls.KotlinCallHierarchyNodeDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import java.util.*

class KotlinCallerChooser(
        declaration: PsiElement,
        project: Project,
        title: String,
        previousTree: Tree?,
        callback: Consumer<Set<PsiElement>>
): CallerChooserBase<PsiElement>(declaration, project, title, previousTree, "dummy." + KotlinFileType.EXTENSION, callback) {
    override fun createTreeNode(method: PsiElement?, called: com.intellij.util.containers.HashSet<PsiElement>, cancelCallback: Runnable): KotlinMethodNode {
        return KotlinMethodNode(method, called, myProject, cancelCallback)
    }

    override fun findDeepestSuperMethods(method: PsiElement) =
            method.toLightMethods().singleOrNull()?.findDeepestSuperMethods()

    override fun getEmptyCallerText() =
            "Caller text \nwith highlighted callee call would be shown here"

    override fun getEmptyCalleeText() =
            "Callee text would be shown here"
}

class KotlinMethodNode(
        method: PsiElement?,
        called: HashSet<PsiElement>,
        project: Project,
        cancelCallback: Runnable
): MethodNodeBase<PsiElement>(method?.namedUnwrappedElement ?: method, called, project, cancelCallback) {
    override fun createNode(caller: PsiElement, called: HashSet<PsiElement>) =
            KotlinMethodNode(caller, called, myProject, myCancelCallback)

    override fun customizeRendererText(renderer: ColoredTreeCellRenderer) {
        val descriptor = when (myMethod) {
            is KtFunction -> myMethod.resolveToDescriptor() as FunctionDescriptor
            is KtClass -> (myMethod.resolveToDescriptor() as ClassDescriptor).unsubstitutedPrimaryConstructor ?: return
            is PsiMethod -> myMethod.getJavaMethodDescriptor() ?: return
            else -> throw AssertionError("Invalid declaration: ${myMethod.getElementTextWithContext()}")
        }
        val containerName = generateSequence<DeclarationDescriptor>(descriptor) { it.containingDeclaration }
                .firstOrNull { it is ClassDescriptor }
                ?.name

        val renderedFunction = KotlinCallHierarchyNodeDescriptor.renderNamedFunction(descriptor)
        val renderedFunctionWithContainer =
                containerName?.let {
                    "${if (it.isSpecial) "[Anonymous]" else it.asString()}.$renderedFunction"
                } ?: renderedFunction

        val attributes = if (isEnabled)
            SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UIUtil.getTreeForeground())
        else
            SimpleTextAttributes.EXCLUDED_ATTRIBUTES
        renderer.append(renderedFunctionWithContainer, attributes)

        val packageName = (myMethod.containingFile as? PsiClassOwner)?.packageName ?: ""
        renderer.append("  ($packageName)", SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, JBColor.GRAY))
    }

    override fun computeCallers(): List<PsiElement> {
        if (myMethod == null) return emptyList()

        val callers = LinkedHashSet<PsiElement>()

        val processor = object: CalleeReferenceProcessor(false) {
            override fun onAccept(ref: PsiReference, element: PsiElement) {
                if ((element is KtFunction || element is KtClass || element is PsiMethod) && element !in myCalled) {
                    callers.add(element)
                }
            }
        }
        val query = myMethod.getRepresentativeLightMethod()?.let { MethodReferencesSearch.search(it, it.useScope, true) }
                    ?: ReferencesSearch.search(myMethod, myMethod.useScope)
        query.forEach { processor.process(it) }
        return callers.toList()
    }
}
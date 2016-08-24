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

package org.jetbrains.kotlin.idea.structureView

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.Queryable
import com.intellij.openapi.util.Computable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.util.ArrayUtil
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.*
import java.util.*

class KotlinStructureViewElement : StructureViewTreeElement, Queryable {
    val element: NavigatablePsiElement
    val isInherited: Boolean

    private var presentation: KotlinStructureElementPresentation? = null

    constructor(element: NavigatablePsiElement, descriptor: DeclarationDescriptor, isInherited: Boolean) {
        this.element = element
        this.isInherited = isInherited

        if (element !is KtElement) {
            // Avoid storing descriptor in fields
            presentation = KotlinStructureElementPresentation(isInherited, element, descriptor)
        }
    }

    constructor(element: NavigatablePsiElement, isInherited: Boolean) {
        this.element = element
        this.isInherited = isInherited
    }

    constructor(fileElement: KtFile) {
        element = fileElement
        isInherited = false
    }

    override fun getValue(): Any {
        return element
    }

    override fun navigate(requestFocus: Boolean) {
        element.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return element.canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
        return element.canNavigateToSource()
    }

    override fun getPresentation(): ItemPresentation {
        if (presentation == null) {
            presentation = KotlinStructureElementPresentation(isInherited, element, descriptor)
        }

        return presentation!!
    }

    override fun getChildren(): Array<TreeElement> {
        val childrenDeclarations = childrenDeclarations
        return ArrayUtil.toObjectArray(ContainerUtil.map(childrenDeclarations, Function<org.jetbrains.kotlin.psi.KtDeclaration, com.intellij.ide.util.treeView.smartTree.TreeElement> { declaration -> KotlinStructureViewElement(declaration, false) }), TreeElement::class.java)
    }

    @TestOnly
    override fun putInfo(info: MutableMap<String, String>) {
        info.put("text", getPresentation().presentableText!!)
        info.put("location", getPresentation().locationString!!)
    }

    private val descriptor: DeclarationDescriptor?
        get() {
            if (!(element.isValid && element is KtDeclaration)) {
                return null
            }

            if (element is KtAnonymousInitializer) {
                return null
            }

            return ApplicationManager.getApplication().runReadAction(Computable<org.jetbrains.kotlin.descriptors.DeclarationDescriptor> {
                if (!DumbService.isDumb(element.getProject())) {
                    return@Computable element.resolveToDescriptor()
                }

                null
            })
        }

    private val childrenDeclarations: List<KtDeclaration>
        get() {
            if (element is KtFile) {
                return element.declarations
            }
            else if (element is KtClass) {
                val declarations = ArrayList<KtDeclaration>()
                for (parameter in element.getPrimaryConstructorParameters()) {
                    if (parameter.hasValOrVar()) {
                        declarations.add(parameter)
                    }
                }
                declarations.addAll(element.declarations)
                return declarations
            }
            else if (element is KtClassOrObject) {
                return element.declarations
            }

            return emptyList()
        }
}

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
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.Queryable
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import javax.swing.Icon

class KotlinStructureViewElement(val element: NavigatablePsiElement,
                                 private val isInherited: Boolean = false) : PsiTreeElementBase<NavigatablePsiElement>(element), Queryable {
    private var _presentation: KotlinStructureElementPresentation? = null
        get() {
            if (field == null) {
                field = KotlinStructureElementPresentation(isInherited, element, _descriptor)
            }

            return field
        }

    private val _descriptor: DeclarationDescriptor?
        get() {
            if (!(element.isValid && element is KtDeclaration)) {
                return null
            }

            if (element is KtAnonymousInitializer) {
                return null
            }

            return runReadAction {
                if (!DumbService.isDumb(element.getProject())) {
                    return@runReadAction element.resolveToDescriptorIfAny()
                }
                null
            }
        }

    val isPublic: Boolean
        get() {
            return (_descriptor as? DeclarationDescriptorWithVisibility)?.visibility == Visibilities.PUBLIC
        }

    constructor(element: NavigatablePsiElement, descriptor: DeclarationDescriptor, isInherited: Boolean) : this(element, isInherited){
        if (element !is KtElement) {
            // Avoid storing descriptor in fields
            _presentation = KotlinStructureElementPresentation(isInherited, element, descriptor)
        }
    }

    override fun getPresentation(): ItemPresentation = _presentation!!
    override fun getLocationString(): String? = _presentation!!.locationString
    override fun getIcon(open: Boolean): Icon? = _presentation!!.getIcon(open)
    override fun getPresentableText(): String? = _presentation!!.presentableText

    @TestOnly
    override fun putInfo(info: MutableMap<String, String?>) {
        // Sanity check for API consistency
        assert(presentation.presentableText == presentableText) { "Two different ways of getting presentableText" }
        assert(presentation.locationString == locationString) { "Two different ways of getting locationString" }

        info["text"] = presentableText
        info["location"] = locationString
    }

    override fun getChildrenBase(): Collection<StructureViewTreeElement> {
        return childrenDeclarations.map { KotlinStructureViewElement(it, false) }
    }

    private val childrenDeclarations: List<KtDeclaration>
        get() = when (element) {
            is KtFile -> element.declarations
            is KtClass -> element.getStructureDeclarations()
            is KtClassOrObject -> element.declarations
            is KtFunction, is KtClassInitializer, is KtProperty -> element.collectLocalDeclarations()
            else -> emptyList()
        }

    private fun PsiElement.collectLocalDeclarations(): List<KtDeclaration> {
        val result = mutableListOf<KtDeclaration>()

        acceptChildren(object : KtTreeVisitorVoid() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                result.add(classOrObject)
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
                result.add(function)
            }
        })

        return result
    }
}

fun KtClassOrObject.getStructureDeclarations() = primaryConstructorParameters.filter { it.hasValOrVar() } + declarations


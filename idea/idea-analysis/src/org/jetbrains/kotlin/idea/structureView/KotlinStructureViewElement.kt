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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class KotlinStructureViewElement(
    element: NavigatablePsiElement,
    private val isInherited: Boolean = false
) : PsiTreeElementBase<NavigatablePsiElement>(element), Queryable {

    private var kotlinPresentation
            by AssignableLazyProperty {
                KotlinStructureElementPresentation(isInherited, element, countDescriptor())
            }

    var visibility
            by AssignableLazyProperty {
                Visibility(countDescriptor())
            }
        private set

    constructor(element: NavigatablePsiElement, descriptor: DeclarationDescriptor, isInherited: Boolean) : this(element, isInherited) {
        if (element !is KtElement) {
            // Avoid storing descriptor in fields
            kotlinPresentation = KotlinStructureElementPresentation(isInherited, element, descriptor)
            visibility = Visibility(descriptor)
        }
    }

    override fun getPresentation(): ItemPresentation = kotlinPresentation
    override fun getLocationString(): String? = kotlinPresentation.locationString
    override fun getIcon(open: Boolean): Icon? = kotlinPresentation.getIcon(open)
    override fun getPresentableText(): String? = kotlinPresentation.presentableText

    @TestOnly
    override fun putInfo(info: MutableMap<String, String?>) {
        // Sanity check for API consistency
        assert(presentation.presentableText == presentableText) { "Two different ways of getting presentableText" }
        assert(presentation.locationString == locationString) { "Two different ways of getting locationString" }

        info["text"] = presentableText
        info["location"] = locationString
    }

    override fun getChildrenBase(): Collection<StructureViewTreeElement> {
        val element = element

        val children = when (element) {
            is KtFile -> element.declarations
            is KtClass -> element.getStructureDeclarations()
            is KtClassOrObject -> element.declarations
            is KtFunction, is KtClassInitializer, is KtProperty -> element.collectLocalDeclarations()
            else -> emptyList()
        }

        return children.map { KotlinStructureViewElement(it, false) }
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

    private fun countDescriptor(): DeclarationDescriptor? {
        val element = element
        return when {
            element == null -> null
            !element.isValid -> null
            element !is KtDeclaration -> null
            element is KtAnonymousInitializer -> null
            else -> runReadAction {
                if (!DumbService.isDumb(element.getProject())) {
                    element.resolveToDescriptorIfAny()
                } else null
            }
        }
    }

    class Visibility(descriptor: DeclarationDescriptor?) {
        private val visibility = (descriptor as? DeclarationDescriptorWithVisibility)?.visibility

        val isPublic: Boolean
            get() = visibility == Visibilities.PUBLIC

        val accessLevel: Int?
            get() = when {
                visibility == Visibilities.PUBLIC -> 1
                visibility == Visibilities.INTERNAL -> 2
                visibility == Visibilities.PROTECTED -> 3
                visibility?.let { Visibilities.isPrivate(it) } == true -> 4
                else -> null
            }
    }
}

private class AssignableLazyProperty<in R, T : Any>(val init: () -> T) : ReadWriteProperty<R, T> {
    private var _value: T? = null

    override fun getValue(thisRef: R, property: KProperty<*>): T {
        return _value ?: init().also { _value = it }
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        _value = value
    }
}

fun KtClassOrObject.getStructureDeclarations() =
    (primaryConstructor?.let { listOf(it) } ?: emptyList()) +
            primaryConstructorParameters.filter { it.hasValOrVar() } +
            declarations


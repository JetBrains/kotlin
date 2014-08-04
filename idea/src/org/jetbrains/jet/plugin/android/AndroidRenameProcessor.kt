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

package org.jetbrains.jet.plugin.android

import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.jet.asJava.*
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.resolve.android.isAndroidSyntheticElement
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlProcessor
import com.intellij.psi.search.SearchScope
import com.intellij.psi.xml.XmlAttributeValue
import org.jetbrains.android.dom.wrappers.LazyValueResourceElementWrapper
import org.jetbrains.android.util.AndroidResourceUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.impl.light.LightElement

public class AndroidRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        // either renaming synthetic property, or value in ui xml, or R light class field
        return (element.namedUnwrappedElement is JetProperty &&
                isAndroidSyntheticElement(element.namedUnwrappedElement)) || element is XmlAttributeValue ||
                element is LightElement
    }

    override fun prepareRenaming(element: PsiElement?, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        if (element?.namedUnwrappedElement is JetProperty) {
            renameSyntheticProperty(element!!.namedUnwrappedElement as JetProperty, newName, allRenames, scope)
        }
        else if (element is XmlAttributeValue) {
            renameAttributeValue(element, newName, allRenames, scope)
        }
        else if (element is LightElement) {
            renameLightClassField(element, newName, allRenames, scope)
        }
    }

    private fun renameSyntheticProperty(jetProperty: JetProperty, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        val oldName = jetProperty.getName()!!
        val processor = ServiceManager.getService(jetProperty.getProject(), javaClass<AndroidUIXmlProcessor>())
        val resourceManager = processor!!.resourceManager
        val attr = resourceManager.idToXmlAttribute(oldName) as XmlAttribute
        allRenames[XmlAttributeValueWrapper(attr.getValueElement()!!)] = resourceManager.nameToId(newName!!)
        val name = AndroidResourceUtil.getResourceNameByReferenceText(newName)
        for (resField in AndroidResourceUtil.findIdFields(attr)) {
            allRenames.put(resField, AndroidResourceUtil.getFieldNameByResourceName(name!!))
        }
        resourceManager.renameProperty(oldName, newName)
    }

    private fun renameAttributeValue(attribute: XmlAttributeValue, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        val element1 = LazyValueResourceElementWrapper.computeLazyElement(attribute);
        val processor = ServiceManager.getService(attribute.getProject(), javaClass<AndroidUIXmlProcessor>())
        if (element1 == null) return
        val oldPropName = AndroidResourceUtil.getResourceNameByReferenceText(attribute.getValue()!!)
        val newPropName = processor!!.resourceManager.idToName(newName!!)
        renameSyntheticProperties(allRenames, newPropName, oldPropName, processor)
    }

    private fun renameSyntheticProperties(allRenames: MutableMap<PsiElement, String>, newPropName: String, oldPropName: String?, processor: AndroidUIXmlProcessor) {
        val props = processor.lastCachedPsi?.findChildrenByClass(javaClass<JetProperty>())
        val matchedProps = props?.filter { it.getName() == oldPropName } ?: arrayListOf()
        for (prop in matchedProps) {
            allRenames[prop] = newPropName
        }
        processor.resourceManager.renameProperty(oldPropName!!, newPropName)
    }

    private fun renameLightClassField(field: LightElement, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        val oldName = field.getName()!!
        val processor = ServiceManager.getService(field.getProject(), javaClass<AndroidUIXmlProcessor>())
        renameSyntheticProperties(allRenames, newName!!, oldName, processor!!)
    }

}

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
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlAttribute

public class AndroidRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        return (element.namedUnwrappedElement is JetProperty &&
                isAndroidSyntheticElement(element.namedUnwrappedElement)) || element is XmlAttributeValue
    }

    override fun prepareRenaming(element: PsiElement?, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        if (element?.namedUnwrappedElement is JetProperty) {
            renameSyntheticProperty(element!!.namedUnwrappedElement as JetProperty, newName, allRenames, scope)
        }
        else if (element is XmlAttributeValue) {
            renameAttributeValue(element, newName, allRenames, scope)
        }
    }

    private fun renameSyntheticProperty(jetProperty: JetProperty, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        val oldName = jetProperty.getName()!!
        val processor = ServiceManager.getService(jetProperty.getProject(), javaClass<AndroidUIXmlProcessor>())
        val resourceManager = processor!!.resourceManager
        val attr = resourceManager.idToXmlAttribute(oldName) as XmlAttribute
        for (file in resourceManager.getLayoutXmlFiles()) {
            if (file is XmlFile) {
                file.accept(object : XmlElementVisitor() {
                    override fun visitElement(element: PsiElement) {
                        element.acceptChildren(this)
                    }
                    override fun visitXmlTag(tag: XmlTag?) {
                        val idPrefix = "@+id/"
                        val attribute = tag?.getAttribute("android:id")
                        if (attribute != null && attribute.getValue() == idPrefix + oldName) {
                            allRenames[XmlAttributeValueWrapper(attribute.getValueElement()!!)] = idPrefix + newName
                        }
                        tag?.acceptChildren(this)
                    }
                })
            }
        }
        val name = AndroidResourceUtil.getResourceNameByReferenceText(newName!!)
        for (resField in AndroidResourceUtil.findIdFields(attr)) {
            allRenames.put(resField, AndroidResourceUtil.getFieldNameByResourceName(name!!))
        }
        resourceManager.renameProperty(oldName, newName)
    }

    private fun renameAttributeValue(attribute: XmlAttributeValue, newName: String?, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
        val element1 = LazyValueResourceElementWrapper.computeLazyElement(attribute);
        if (element1 == null) return
        val id = AndroidResourceUtil.getResourceNameByReferenceText(attribute.getValue()!!)
    }
}

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

import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlElement
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlAttribute
import org.jetbrains.jet.lang.resolve.android.AndroidResourceManager

class AndroidXmlVisitor(val resourceManager: AndroidResourceManager, val elementCallback: (String, String, XmlAttribute) -> Unit) : XmlElementVisitor() {

    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }

    override fun visitXmlElement(element: XmlElement?) {
        element?.acceptChildren(this)
    }

    override fun visitXmlTag(tag: XmlTag?) {
        val attribute = tag?.getAttribute(resourceManager.idAttribute)
        if (attribute != null && attribute.getValue() != null) {
            val classNameAttr = tag?.getAttribute(resourceManager.classAttributeNoNamespace)?.getValue() ?: tag?.getLocalName()
            elementCallback(resourceManager.idToName(attribute.getValue()), classNameAttr!!, attribute)
        }
        tag?.acceptChildren(this)
    }
}


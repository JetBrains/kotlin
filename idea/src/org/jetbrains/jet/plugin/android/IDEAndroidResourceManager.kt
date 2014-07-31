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

import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.resolve.android.AndroidResourceManagerBase
import com.intellij.psi.PsiElement
import java.util.HashMap
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.xml.XmlTag

public class IDEAndroidResourceManager(project: Project, searchPath: String?) : AndroidResourceManagerBase(project, searchPath) {

    private val idToXmlAttributeCache = HashMap<String, PsiElement>()

            ;{
        setupElementCache()
    }

    private fun setupElementCache() {
        idToXmlAttributeCache.clear()
        for (file in getLayoutXmlFiles()) {
            if (file is XmlFile) {
                file.accept(object : XmlElementVisitor() {
                    override fun visitElement(element: PsiElement) {
                        element.acceptChildren(this)
                    }
                    override fun visitXmlTag(tag: XmlTag?) {
                        val attribute = tag?.getAttribute("android:id")
                        val s = attribute?.getValue()
                        if (attribute != null && s != null && isResourceId(s)) {
                            idToXmlAttributeCache[idToName(s)] = attribute
                        }
                        tag?.acceptChildren(this)
                    }
                })
            }
        }

    }
    override fun idToXmlAttribute(id: String): PsiElement? {
        val element = idToXmlAttributeCache[id]
        // element not in cache - files might have changed
        if (element == null) {
            setupElementCache()
            return idToXmlAttributeCache[id]
        }
        return element
    }
    override fun renameXmlAttr(elem: PsiElement, newName: String) {
        val xmlAttr = elem as XmlAttribute
        idToXmlAttributeCache.remove(xmlAttr.getName())
        idToXmlAttributeCache[newName] = xmlAttr
    }
    override fun renameProperty(oldName: String, newName: String) {
        val oldElem = idToXmlAttributeCache[oldName]
        idToXmlAttributeCache.remove(oldName)
        idToXmlAttributeCache[newName] = oldElem!!
    }
}

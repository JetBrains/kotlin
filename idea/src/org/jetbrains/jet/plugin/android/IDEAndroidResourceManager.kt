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
import com.intellij.psi.PsiFile
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.jet.lang.resolve.android.AndroidManifest
import com.intellij.openapi.module.ModuleManager
import java.util.ArrayList

public class IDEAndroidResourceManager(project: Project, searchPath: String?) : AndroidResourceManagerBase(project, searchPath) {

    private class NoAndroidFacetException: Exception("No android facet found in project")

    // TODO: synchronize!
    // TODO: invalidate on modification?
    private val idToXmlAttributeCache = HashMap<String, PsiElement>()

    override fun getLayoutXmlFiles(): Collection<PsiFile> {
        try {
            val facet = getAndroidFacet()
            return facet.getAllResourceDirectories() flatMap { it.findChild("layout")?.getChildren()!! map { vritualFileToPsi(it)!! } }
        } catch (e: NoAndroidFacetException) {
            return ArrayList(0)
        }
    }

    private fun getAndroidFacet(): AndroidFacet {
        for (module in ModuleManager.getInstance(project)!!.getModules()) {
            val facet = AndroidFacet.getInstance(module)
            if (facet != null) return facet
        }
        throw NoAndroidFacetException()
    }

    override fun readManifest(): AndroidManifest {
        val facet = getAndroidFacet()
        val attributeValue = facet.getManifest()!!.getPackage()
        return AndroidManifest(attributeValue!!.getRawText()!!)
    }

    public fun addMapping(name: String, attr: XmlAttribute) {
        idToXmlAttributeCache[name] = attr
    }

    public fun resetAttributeCache() {
        idToXmlAttributeCache.clear()
    }

    override fun idToXmlAttribute(id: String): PsiElement? {
        val element = idToXmlAttributeCache[id]
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

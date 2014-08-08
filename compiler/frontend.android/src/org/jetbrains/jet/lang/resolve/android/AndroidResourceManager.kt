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

package org.jetbrains.jet.lang.resolve.android

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project

abstract class AndroidResourceManager(val project: Project, val searchPath: String?) {
    private val idDeclarationPrefix = "@+id/"
    private val idUsagePrefix = "@id/"
    public val androidNamespace: String = "android"
    public val idAttributeNoNamespace: String = "id"
    public val idAttribute: String = androidNamespace + ":" + idAttributeNoNamespace
    public val classAttributeNoNamespace: String = "class"
    public val classAttribute: String = androidNamespace + ":" + classAttributeNoNamespace

    abstract fun getLayoutXmlFiles(): Collection<PsiFile>
    abstract fun idToXmlAttribute(id: String): PsiElement?
    abstract fun renameXmlAttr(elem: PsiElement, newName: String)
    abstract fun renameProperty(oldName: String, newName: String)
    public fun nameToIdDeclaration(name: String): String = idDeclarationPrefix + name
    public fun nameToIdUsage(name: String): String = idUsagePrefix + name
    public fun idToName(id: String?): String {
        return if (isResourceIdDeclaration(id)) id!!.replace(idDeclarationPrefix, "")
        else if (isResourceIdUsage(id)) id!!.replace(idUsagePrefix, "")
        else throw WrongIdFormat(id)
    }
    public fun isResourceIdDeclaration(str: String?): Boolean = str?.startsWith(idDeclarationPrefix) ?: false
    public fun isResourceIdUsage(str: String?): Boolean = str?.startsWith(idUsagePrefix) ?: false
    public fun isResourceDeclarationOrUsage(id: String?): Boolean = isResourceIdDeclaration(id) || isResourceIdUsage(id)

    abstract fun readManifest(): AndroidManifest

    inner class NoUIXMLsFound : Exception("No android UI xmls found in $searchPath")
    inner class WrongIdFormat(id: String?) : Exception("Id \"$id\" has wrong format")

}

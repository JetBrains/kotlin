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
    private val idPrefix = "@+id/"

    abstract fun getLayoutXmlFiles(): Collection<PsiFile>
    abstract fun idToXmlAttribute(id: String): PsiElement?
    abstract fun renameXmlAttr(elem: PsiElement, newName: String)
    abstract fun renameProperty(oldName: String, newName: String)
    public fun nameToId(name: String): String = idPrefix + name
    public fun idToName(id: String): String = id.replace(idPrefix, "")
    public fun isResourceId(str: String?): Boolean = str?.startsWith(idPrefix) ?: false

    abstract fun readManifest(): AndroidManifest

    inner class NoUIXMLsFound : Exception("No android UI xmls found in $searchPath")
}

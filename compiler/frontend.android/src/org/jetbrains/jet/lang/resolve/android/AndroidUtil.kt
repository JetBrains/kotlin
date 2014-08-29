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
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.openapi.components.ServiceManager
import java.util.ArrayList

trait AndroidResource

class AndroidID(val rawID: String) : AndroidResource {

    override fun equals(other: Any?): Boolean {
        return other is AndroidID && this.rawID == other.rawID
    }
    override fun hashCode(): Int {
        return rawID.hashCode()
    }
    override fun toString(): String {
        return rawID
    }
}

class AndroidWidget(val id: String, val className: String) : AndroidResource

class AndroidManifest(val _package: String) : AndroidResource

fun isAndroidSyntheticFile(f: PsiFile?): Boolean {
    return f?.getUserData(AndroidConst.ANDROID_USER_PACKAGE) != null
}

public fun isAndroidSyntheticElement(element: PsiElement?): Boolean {
    return isAndroidSyntheticFile(element?.getContainingFile())
}
public fun searchAndAddAndroidDeclarations(project: Project, originalFiles: Collection<JetFile>): Collection<JetFile> {
    val parser = ServiceManager.getService<AndroidUIXmlProcessor>(project, javaClass<AndroidUIXmlProcessor>())
    val file = parser?.parseToPsi(project)
    val files = ArrayList(originalFiles)
    if (file != null) files.add(file)
    return files
}

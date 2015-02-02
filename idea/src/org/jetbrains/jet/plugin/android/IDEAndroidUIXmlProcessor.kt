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

import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlProcessor
import com.intellij.openapi.project.Project
import java.util.ArrayList
import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.resolve.android.AndroidWidget
import org.jetbrains.jet.lang.resolve.android.KotlinStringWriter

class IDEAndroidUIXmlProcessor(project: Project) : AndroidUIXmlProcessor(project) {
    override val searchPath: String? = project.getBasePath() + "/res/layout/"
    override var androidAppPackage: String = ""

    override val resourceManager: IDEAndroidResourceManager = IDEAndroidResourceManager(project, searchPath)

    override protected fun lazySetup() {
        if (listenerSetUp) return
        androidAppPackage = resourceManager.readManifest()._package
        populateQueue()
        listenerSetUp = true
    }

    override fun parseSingleFileImpl(file: PsiFile): String {
        val ids: MutableCollection<AndroidWidget> = ArrayList()
        resourceManager.resetAttributeCache()
        file.accept(AndroidXmlVisitor { id, wClass, valueElement ->
            ids.add(AndroidWidget(id, wClass))
            resourceManager.addMapping(id, valueElement)
        })
        return produceKotlinProperties(KotlinStringWriter(), ids).toString()
    }
}


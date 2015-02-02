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

import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import java.util.ArrayList
import com.intellij.openapi.vfs.VirtualFileAdapter
import com.intellij.openapi.vfs.VirtualFileEvent

class IDEAndroidUIXmlParser(project: Project): AndroidUIXmlParser() {
    override val searchPath: String? = project.getBasePath() + "/res/layout/"
    override var androidAppPackage: String = ""

    override protected fun lazySetup() {
        if (listenerSetUp) return
        androidAppPackage = readManifest()._package
        val fileManager = VirtualFileManager.getInstance()
        val watchDir = fileManager.findFileByUrl("file://" + searchPath)
        filesToProcess.addAll(watchDir?.getChildren()?.toArrayList() ?: ArrayList(0))
        fileManager.addVirtualFileListener(object : VirtualFileAdapter() {
            override fun contentsChanged(event: VirtualFileEvent) {
                if (event.getParent() == watchDir)
                    filesToProcess.add(event.getFile())
            }
            override fun fileCreated(event: VirtualFileEvent) {
                if (event.getParent() == watchDir)
                    super<VirtualFileAdapter>.fileCreated(event)
            }
            override fun fileDeleted(event: VirtualFileEvent) {
                if (event.getParent() == watchDir) {
                    // ignore potential synchronisation issues - it doesn't really matter if invalidation and
                    // file processing will be handled in different passes
                    invalidateCaches = true
                    filesToProcess.addAll(watchDir?.getChildren()?.toArrayList() ?: ArrayList(0))
                }
            }
        })
        listenerSetUp = true
    }
}


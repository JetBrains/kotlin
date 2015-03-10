/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter
import com.intellij.openapi.vfs.VirtualFileAdapter
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.openapi.roots.ProjectFileIndex
import kotlin.platform.platformStatic
import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.codeInsight.ExternalAnnotationsListener
import com.intellij.psi.PsiModifierListOwner

class LibraryModificationTracker(project: Project) : SimpleModificationTracker() {
    default object {
        platformStatic fun getInstance(project: Project) = ServiceManager.getService(project, javaClass<LibraryModificationTracker>())!!
    }

    {
        val connection = project.getMessageBus().connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, BulkVirtualFileListenerAdapter(
                object : VirtualFileAdapter() {
                    override fun fileCreated(event: VirtualFileEvent) = processEvent(event)
                    override fun beforeFileMovement(event: VirtualFileMoveEvent) = processEvent(event)
                    override fun beforeContentsChange(event: VirtualFileEvent) = processEvent(event)
                    override fun beforeFileDeletion(event: VirtualFileEvent) = processEvent(event)
                }
        ))

        connection.subscribe(ExternalAnnotationsManager.TOPIC, object : ExternalAnnotationsListener {
            override fun afterExternalAnnotationChanging(owner: PsiModifierListOwner, annotationFQName: String, successful: Boolean) {
                if (successful) incModificationCount()
            }

            override fun externalAnnotationsChangedExternally() = incModificationCount()
        })
    }

    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    fun processEvent(event: VirtualFileEvent) {
        if (projectFileIndex.isInLibraryClasses(event.getFile())) {
            incModificationCount()
        }
    }
}


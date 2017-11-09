/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileElement
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.*
import com.intellij.openapi.roots.libraries.ui.FileTypeBasedRootFilter
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent
import com.intellij.openapi.roots.libraries.ui.RootDetector
import com.intellij.openapi.roots.ui.configuration.libraryEditor.DefaultLibraryRootsComponentDescriptor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.JComponent

class JSLibraryType : LibraryType<DummyLibraryProperties>(JSLibraryKind) {
    override fun createPropertiesEditor(editorComponent: LibraryEditorComponent<DummyLibraryProperties>) = null

    override fun getCreateActionName() = "Kotlin/JS"

    override fun createNewLibrary(parentComponent: JComponent,
                                  contextDirectory: VirtualFile?,
                                  project: Project): NewLibraryConfiguration? {
        return LibraryTypeService.getInstance().createLibraryFromFiles(RootsComponentDescriptor,
                                                                       parentComponent, contextDirectory, this,
                                                                       project)
    }

    override fun getIcon(properties: DummyLibraryProperties?) = KotlinIcons.JS

    companion object {
        fun getInstance() = Extensions.findExtension(EP_NAME, JSLibraryType::class.java)
    }

    object RootsComponentDescriptor : DefaultLibraryRootsComponentDescriptor() {
        override fun createAttachFilesChooserDescriptor(libraryName: String?): FileChooserDescriptor {
            val descriptor = FileChooserDescriptor(true, true, true, false, true, true).withFileFilter {
                FileElement.isArchive(it) || isAcceptedForJsLibrary(it.extension)
            }
            descriptor.title = if (StringUtil.isEmpty(libraryName))
                ProjectBundle.message("library.attach.files.action")
            else
                ProjectBundle.message("library.attach.files.to.library.action", libraryName!!)
            descriptor.description = ProjectBundle.message("library.java.attach.files.description")
            return descriptor
        }

        override fun getRootTypes() = arrayOf(OrderRootType.CLASSES, OrderRootType.SOURCES)

        override fun getRootDetectors(): List<RootDetector> {
            return arrayListOf(
                    JSRootFilter,
                    FileTypeBasedRootFilter(OrderRootType.SOURCES, false, KotlinFileType.INSTANCE, "sources")
            )
        }
    }

    object JSRootFilter : FileTypeBasedRootFilter(OrderRootType.CLASSES, false, PlainTextFileType.INSTANCE, "JS files") {
        override fun isFileAccepted(virtualFile: VirtualFile) = isAcceptedForJsLibrary(virtualFile.extension)

    }
}

private fun isAcceptedForJsLibrary(extension: String?) = extension == "js" || extension == "kjsm"

val TargetPlatformKind<*>.libraryKind: PersistentLibraryKind<*>?
    get() = when(this) {
        TargetPlatformKind.JavaScript -> JSLibraryKind
        TargetPlatformKind.Common -> CommonLibraryKind
        else -> null
    }

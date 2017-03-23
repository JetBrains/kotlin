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

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.LibraryProperties
import com.intellij.openapi.roots.libraries.LibraryType
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.JComponent

object CommonLibraryType : LibraryType<DummyLibraryProperties>(CommonLibraryKind) {
    override fun createPropertiesEditor(editorComponent: LibraryEditorComponent<DummyLibraryProperties>) = null

    override fun getCreateActionName() = null

    override fun createNewLibrary(parentComponent: JComponent,
                                  contextDirectory: VirtualFile?,
                                  project: Project): NewLibraryConfiguration? = null

    override fun getIcon(properties: LibraryProperties<*>?) = KotlinIcons.SMALL_LOGO
}

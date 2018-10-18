/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
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

    override fun getIcon(properties: DummyLibraryProperties?) = KotlinIcons.MPP
}

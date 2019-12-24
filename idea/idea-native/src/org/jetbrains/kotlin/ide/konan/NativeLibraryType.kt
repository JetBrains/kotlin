/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.LibraryType
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinIcons
import javax.swing.Icon
import javax.swing.JComponent

object NativeLibraryType : LibraryType<DummyLibraryProperties>(NativeLibraryKind) {
    override fun createPropertiesEditor(editorComponent: LibraryEditorComponent<DummyLibraryProperties>): Nothing? = null
    override fun getCreateActionName(): Nothing? = null
    override fun createNewLibrary(parentComponent: JComponent, contextDirectory: VirtualFile?, project: Project): Nothing? = null

    // Library type is determined by `KotlinGradleLibraryDataService` for every library dependency imported from Gradle to IDE.
    // However this does not work for libraries that are to be just created during project build, e.g. C-interop Kotlin/Native KLIBs.
    // The code below helps to perform postponed detection of Kotlin/Native libraries.
    override fun detect(classesRoots: List<VirtualFile>): DummyLibraryProperties? =
        if (classesRoots.firstOrNull()?.isKonanLibraryRoot == true)
            DummyLibraryProperties.INSTANCE!!
        else null

    override fun getIcon(properties: DummyLibraryProperties?): Icon = KotlinIcons.NATIVE
}

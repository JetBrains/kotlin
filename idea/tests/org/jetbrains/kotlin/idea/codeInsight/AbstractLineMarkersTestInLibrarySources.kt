/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.util.io.createFile
import com.intellij.util.io.write
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import java.io.File
import java.nio.file.Files

abstract class AbstractLineMarkersTestInLibrarySources : AbstractLineMarkersTest() {

    private var libraryCleanPath: String? = null

    private var libraryClean: File? = null

    private fun getLibraryCleanPath(): String = libraryCleanPath!!

    private fun getLibraryOriginalPath(): String = PluginTestCaseBase.getTestDataPathBase() + "/codeInsightInLibrary/_library"

    override fun getProjectDescriptor(): SdkAndMockLibraryProjectDescriptor {
        if (libraryCleanPath == null) {
            val libraryClean = Files.createTempDirectory("lineMarkers_library")
            val libraryOriginal = File(getLibraryOriginalPath())
            libraryCleanPath = libraryClean.toString()

            for (file in libraryOriginal.walkTopDown().filter { !it.isDirectory }) {
                val text = file.readText().replace("</?lineMarker.*?>".toRegex(), "")
                val cleanFile = libraryClean.resolve(file.relativeTo(libraryOriginal).path)
                cleanFile.createFile()
                cleanFile.write(text)
            }
            this.libraryClean = File(libraryCleanPath)
        }
        return object : SdkAndMockLibraryProjectDescriptor(getLibraryCleanPath(), false) {
            override fun configureModule(module: Module, model: ModifiableRootModel) {
                super.configureModule(module, model)

                val library = model.moduleLibraryTable.getLibraryByName(SdkAndMockLibraryProjectDescriptor.LIBRARY_NAME)!!
                val modifiableModel = library.modifiableModel

                modifiableModel.addRoot(LocalFileSystem.getInstance().findFileByIoFile(libraryClean!!)!!, OrderRootType.SOURCES)
                modifiableModel.commit()
            }
        }
    }

    fun doTestWithLibrary(path: String) {
        doTest(path) {
            val fileSystem = VirtualFileManager.getInstance().getFileSystem("file")
            val libraryOriginal = File(getLibraryOriginalPath())
            val project = myFixture.project
            for (file in libraryOriginal.walkTopDown().filter { !it.isDirectory }) {
                myFixture.openFileInEditor(fileSystem.findFileByPath(file.absolutePath)!!)
                val data = ExpectedHighlightingData(
                    myFixture.editor.document, false, false, false, myFixture.file
                )
                data.init()

                val librarySourceFile = libraryClean!!.resolve(file.relativeTo(libraryOriginal).path)
                myFixture.openFileInEditor(fileSystem.findFileByPath(librarySourceFile.absolutePath)!!)
                val document = myFixture.editor.document
                PsiDocumentManager.getInstance(project).commitAllDocuments()

                if (!ProjectRootsUtil.isLibrarySourceFile(project, myFixture.file.virtualFile)) {
                    throw AssertionError("File ${myFixture.file.virtualFile.path} should be in library sources!")
                }

                doAndCheckHighlighting(document, data, file)
            }
        }
    }

    override fun tearDown() {
        libraryClean?.deleteRecursively()
        ConfigLibraryUtil.removeLibrary(module, SdkAndMockLibraryProjectDescriptor.LIBRARY_NAME)

        super.tearDown()
    }
}
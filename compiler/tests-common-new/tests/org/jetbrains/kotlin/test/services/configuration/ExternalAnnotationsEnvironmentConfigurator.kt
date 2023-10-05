/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import com.intellij.codeInsight.BaseExternalAnnotationsManager
import com.intellij.codeInsight.ExternalAnnotationsManager
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceFileProvider

/**
 * Adds the ability to declare 'annotations.xml' file with description of external annotations.
 *
 * Such synthetic Java annotations can be found by
 * [JavaAnnotation.isIdeExternalAnnotation][org.jetbrains.kotlin.load.java.structure.JavaAnnotation.isIdeExternalAnnotation] flag.
 *
 * @see ExternalAnnotationsManager
 * @see org.jetbrains.kotlin.test.preprocessors.ExternalAnnotationsSourcePreprocessor
 */
class ExternalAnnotationsEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun legacyRegisterCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        var hasAnnotationFile = false
        for (file in module.files) {
            if (file.name != ExternalAnnotationsManager.ANNOTATIONS_XML) continue

            // This call is required to copy files into the project to be able to read those xml files from the manager
            testServices.sourceFileProvider.getRealFileForSourceFile(file)
            hasAnnotationFile = true
        }

        if (!hasAnnotationFile) return

        (project as MockProject).picoContainer.unregisterComponent(ExternalAnnotationsManager::class.java.name)
        project.registerService(
            ExternalAnnotationsManager::class.java,
            ExternalAnnotationsManagerForTests(
                // We use additionalFilesDirectory as a root of external annotations because this directory contains all
                // declared 'annotations.xml' files
                externalAnnotationsRootPath = testServices.sourceFileProvider.additionalFilesDirectory.path,
                manager = PsiManager.getInstance(project),
            ),
        )
    }
}

/**
 * Simple [ExternalAnnotationsManager] implementation
 * which always suggests searching external annotations inside [externalAnnotationsRootPath].
 *
 * @param externalAnnotationsRootPath path to the directory with 'annotations.xml' files
 */
private class ExternalAnnotationsManagerForTests(
    externalAnnotationsRootPath: String,
    manager: PsiManager,
) : BaseExternalAnnotationsManager(manager) {
    private val externalAnnotationsRoots: List<VirtualFile> by lazyPub {
        VirtualFileManager.getInstance()
            .getFileSystem(StandardFileSystems.FILE_PROTOCOL)
            .findFileByPath(externalAnnotationsRootPath)
            ?.let(::listOf)
            ?: error("File with external annotations is not found")
    }

    /**
     * We simply returns [externalAnnotationsRoots] because there is all our declared 'annotations.xml' files
     *
     * @param libraryFile is a file for which we want to find the corresponding external annotations file if it exists
     */
    override fun getExternalAnnotationsRoots(libraryFile: VirtualFile): List<VirtualFile> = externalAnnotationsRoots

    override fun hasAnyAnnotationsRoots(): Boolean = true
    override fun hasConfiguredAnnotationRoot(owner: PsiModifierListOwner): Boolean = true
}
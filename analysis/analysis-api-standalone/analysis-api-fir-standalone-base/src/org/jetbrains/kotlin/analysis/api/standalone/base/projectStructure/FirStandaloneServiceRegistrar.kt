/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure

import com.intellij.core.CoreFileTypeRegistry
import com.intellij.diagnostic.PluginException
import com.intellij.diagnostic.PluginProblemReporter
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.psi.ClassFileViewProviderFactory
import com.intellij.psi.FileTypeFileViewProviders
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.compiled.ClassFileDecompiler
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.BinaryFileStubBuilders
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetaFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

@OptIn(KaImplementationDetail::class)
object FirStandaloneServiceRegistrar : AnalysisApiSimpleServiceRegistrar() {
    private const val PLUGIN_RELATIVE_PATH = "/META-INF/analysis-api/analysis-api-fir-standalone-base.xml"

    override fun registerApplicationServices(application: MockApplication, disposable: Disposable) {
        with(FileTypeRegistry.getInstance() as CoreFileTypeRegistry) {
            registerFileType(KlibMetaFileType, KLIB_METADATA_FILE_EXTENSION)
            registerFileType(KotlinBuiltInFileType, BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION)
            registerFileType(KotlinBuiltInFileType, METADATA_FILE_EXTENSION)
        }
        PluginStructureProvider.registerApplicationServices(application, PLUGIN_RELATIVE_PATH)

        for (fileType in listOf(JavaClassFileType.INSTANCE, KotlinBuiltInFileType, KlibMetaFileType)) {
            FileTypeFileViewProviders.INSTANCE.addExplicitExtension(fileType, ClassFileViewProviderFactory(), disposable)
            BinaryFileStubBuilders.INSTANCE.addExplicitExtension(fileType, ClassFileStubBuilder(), disposable)
            BinaryFileTypeDecompilers.getInstance().addExplicitExtension(fileType, ClassFileDecompiler(), disposable)
        }

        // To properly handle exceptions from the stub builder
        @Suppress("UnstableApiUsage")
        application.registerService(
            PluginProblemReporter::class.java,
            PluginProblemReporter { errorMessage, cause, _ ->
                PluginException(errorMessage, cause, null)
            },
        )
    }

    override fun registerProjectExtensionPoints(project: MockProject) {
    }

    override fun registerProjectServices(project: MockProject) {
        PluginStructureProvider.registerProjectServices(project, PLUGIN_RELATIVE_PATH)
    }

    @OptIn(KaExperimentalApi::class)
    @Suppress("TestOnlyProblems")
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable) {
        with(PsiElementFinder.EP.getPoint(project)) {
            registerExtension(JavaElementFinder(project), disposable)
            registerExtension(PsiElementFinderImpl(project), disposable)
        }
    }
}

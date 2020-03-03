/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.ide.konan.decompiler.KlibLoadingMetadataCache
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.KotlinLibraryImpl
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.konan.file.File as KFile

fun createFileStub(project: Project, text: String): PsiFileStub<*> {
    val virtualFile = LightVirtualFile("dummy.kt", KotlinFileType.INSTANCE, text)
    virtualFile.language = KotlinLanguage.INSTANCE
    SingleRootFileViewProvider.doNotCheckFileSizeLimit(virtualFile)

    val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
    val file = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, false, false)!!
    return KtStubElementTypes.FILE.builder.buildStubTree(file) as PsiFileStub<*>
}

fun createLoggingErrorReporter(log: Logger) = LoggingErrorReporter(log)

internal object CachingIdeKonanLibraryMetadataLoader : PackageAccessHandler {
    override fun loadModuleHeader(library: KotlinLibrary): KlibMetadataProtoBuf.Header {
        val virtualFile = getVirtualFile(library, library.moduleHeaderFile)
        return virtualFile?.let { cache.getCachedModuleHeader(virtualFile) } ?: KlibMetadataProtoBuf.Header.getDefaultInstance()
    }

    override fun loadPackageFragment(library: KotlinLibrary, packageFqName: String, partName: String): ProtoBuf.PackageFragment {
        val virtualFile = getVirtualFile(library, library.packageFragmentFile(packageFqName, partName))
        return virtualFile?.let { cache.getCachedPackageFragment(virtualFile) } ?: ProtoBuf.PackageFragment.getDefaultInstance()
    }

    private fun getVirtualFile(library: KotlinLibrary, file: KFile): VirtualFile? =
        if (library.isZipped) asJarFileSystemFile(library.libraryFile, file) else asLocalFile(file)

    private fun asJarFileSystemFile(jarFile: KFile, localFile: KFile): VirtualFile? {
        val fullPath = jarFile.absolutePath + "!" + PathUtil.toSystemIndependentName(localFile.path)
        return StandardFileSystems.jar().findFileByPath(fullPath)
    }

    private fun asLocalFile(localFile: KFile): VirtualFile? {
        val fullPath = localFile.absolutePath
        return StandardFileSystems.local().findFileByPath(fullPath)
    }

    private val cache
        get() = KlibLoadingMetadataCache.getInstance()

    private val KotlinLibrary.moduleHeaderFile
        get() = (this as KotlinLibraryImpl).metadata.access.layout.moduleHeaderFile

    private fun KotlinLibrary.packageFragmentFile(packageFqName: String, partName: String) =
        (this as KotlinLibraryImpl).metadata.access.layout.packageFragmentFile(packageFqName, partName)

    private val KotlinLibrary.isZipped
        get() = (this as KotlinLibraryImpl).base.access.layout.isZipped
}

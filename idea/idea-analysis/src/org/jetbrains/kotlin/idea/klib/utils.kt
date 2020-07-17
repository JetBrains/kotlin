/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.klib

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataModuleDescriptorFactory
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.storage.StorageManager
import java.io.IOException
import java.util.*

fun VirtualFile.isKlibLibraryRootForPlatform(targetPlatform: TargetPlatform): Boolean {
    // The virtual file for a library packed in a ZIP file will have path like "/some/path/to/the/file.klib!/",
    // and therefore will be recognized by VFS as a directory (isDirectory == true).
    // So, first, let's check the file type and file extension.
    if ((fileType == ArchiveFileType.INSTANCE && extension != KLIB_FILE_EXTENSION) || !isDirectory)
        return false

    // run check for library root too
    // this is necessary to recognize old style KLIBs that do not have components, and report tem to user appropriately
    // (relevant only for Kotlin/Native KLIBs)
    val requestedBuiltInsPlatform = targetPlatform.toBuiltInsPlatform()
    if (requestedBuiltInsPlatform == BuiltInsPlatform.NATIVE && checkKlibComponent(this, requestedBuiltInsPlatform)) {
        return true
    }

    return children?.any { checkKlibComponent(it, requestedBuiltInsPlatform) } == true
}

private fun checkKlibComponent(componentFile: VirtualFile, requestedBuiltInsPlatform: BuiltInsPlatform): Boolean {
    val manifestFile = componentFile.findChild(KLIB_MANIFEST_FILE_NAME)?.takeIf { !it.isDirectory } ?: return false

    val manifestProperties = try {
        manifestFile.inputStream.use { Properties().apply { load(it) } }
    } catch (_: IOException) {
        return false
    }

    if (!manifestProperties.containsKey(KLIB_PROPERTY_UNIQUE_NAME)) return false

    val builtInsPlatformProperty = manifestProperties.getProperty(KLIB_PROPERTY_BUILTINS_PLATFORM)
    // No builtins_platform property => either a new common klib (we don't write builtins_platform for common) or old Native klib
        ?: return when (requestedBuiltInsPlatform) {
            BuiltInsPlatform.NATIVE -> componentFile.isLegacyNativeKlibComponent // TODO(dsavvinov): drop additional legacy check after 1.4
            BuiltInsPlatform.COMMON -> !componentFile.isLegacyNativeKlibComponent
            else -> false
        }

    val builtInsPlatform = BuiltInsPlatform.parseFromString(builtInsPlatformProperty) ?: return false

    return builtInsPlatform == requestedBuiltInsPlatform
}

private fun TargetPlatform.toBuiltInsPlatform() = when {
    isCommon() -> BuiltInsPlatform.COMMON
    isNative() -> BuiltInsPlatform.NATIVE
    isJvm() -> BuiltInsPlatform.JVM
    isJs() -> BuiltInsPlatform.JS
    else -> throw IllegalArgumentException("Unknown platform $this")
}

private val VirtualFile.isLegacyNativeKlibComponent: Boolean
    get() {
        val irFolder = findChild(KLIB_IR_FOLDER_NAME)
        return irFolder != null && irFolder.children.isNotEmpty()
    }


fun <T> KotlinLibrary.safeRead(defaultValue: T, action: KotlinLibrary.() -> T) = try {
    action()
} catch (_: IOException) {
    defaultValue
}

fun createFileStub(project: Project, text: String): PsiFileStub<*> {
    val virtualFile = LightVirtualFile("dummy.kt", KotlinFileType.INSTANCE, text)
    virtualFile.language = KotlinLanguage.INSTANCE
    SingleRootFileViewProvider.doNotCheckFileSizeLimit(virtualFile)

    val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
    val file = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, false, false)!!
    return KtStubElementTypes.FILE.builder.buildStubTree(file) as PsiFileStub<*>
}

fun KotlinLibrary.createKlibPackageFragmentProvider(
    storageManager: StorageManager,
    metadataModuleDescriptorFactory: KlibMetadataModuleDescriptorFactory,
    languageVersionSettings: LanguageVersionSettings,
    moduleDescriptor: ModuleDescriptor,
    lookupTracker: LookupTracker
): PackageFragmentProvider? {
    if (!getCompatibilityInfo().isCompatible) return null

    val packageFragmentNames = CachingIdeKlibMetadataLoader.loadModuleHeader(this).packageFragmentNameList

    return metadataModuleDescriptorFactory.createPackageFragmentProvider(
        library = this,
        packageAccessHandler = CachingIdeKlibMetadataLoader,
        packageFragmentNames = packageFragmentNames,
        storageManager = storageManager,
        moduleDescriptor = moduleDescriptor,
        configuration = CompilerDeserializationConfiguration(languageVersionSettings),
        compositePackageFragmentAddend = null,
        lookupTracker = lookupTracker
    )
}

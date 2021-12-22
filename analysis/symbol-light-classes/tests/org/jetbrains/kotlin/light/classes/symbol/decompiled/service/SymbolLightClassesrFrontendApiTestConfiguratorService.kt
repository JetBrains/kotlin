/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled.service

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProviderFactory
import com.intellij.psi.FileTypeFileViewProviders
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.kotlin.analysis.api.fir.FirFrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.decompiled.light.classes.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.analysis.decompiler.stub.file.CachedAttributeData
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.FileAttributeService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import java.io.DataInput
import java.io.DataOutput

object SymbolLightClassesForLibraryFrontendApiTestConfiguratorService : FrontendApiTestConfiguratorService {
    override fun TestConfigurationBuilder.configureTest(disposable: Disposable) {
        usePreAnalysisHandlers(::LibraryModuleRegistrarPreAnalysisHandler)
    }

    override fun registerProjectServices(project: MockProject) {
        project.registerService(ClsJavaStubByVirtualFileCache::class.java)
    }


    override fun registerApplicationServices(application: MockApplication) {
        FirFrontendApiTestConfiguratorService.registerApplicationServices(application)
        if (application.getServiceIfCreated(ClsKotlinBinaryClassCache::class.java) != null) return
        application.registerService(ClsKotlinBinaryClassCache::class.java)
        application.registerService(FileAttributeService::class.java, DummyFileAttributeService)
        FileTypeFileViewProviders.INSTANCE.addExplicitExtension(
            JavaClassFileType.INSTANCE,
            ClassFileViewProviderFactory()
        )
        @Suppress("DEPRECATION")
        ClassFileDecompilers.getInstance().EP_NAME.point.registerExtension(KotlinClassFileDecompiler(), LoadingOrder.FIRST)
        @Suppress("DEPRECATION")
        ClassFileDecompilers.getInstance().EP_NAME.point.registerExtension(KotlinBuiltInDecompiler(), LoadingOrder.FIRST)
    }

    override fun doOutOfBlockModification(file: KtFile) {
        FirFrontendApiTestConfiguratorService.doOutOfBlockModification(file)
    }
}

private object DummyFileAttributeService : FileAttributeService {
    override fun <T> write(file: VirtualFile, id: String, value: T, writeValueFun: (DataOutput, T) -> Unit): CachedAttributeData<T> {
        return CachedAttributeData(value, 0)
    }

    override fun <T> read(file: VirtualFile, id: String, readValueFun: (DataInput) -> T): CachedAttributeData<T>? {
        return null
    }
}

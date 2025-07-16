/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.mock.MockApplication
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers
import com.intellij.psi.ClassFileViewProviderFactory
import com.intellij.psi.FileTypeFileViewProviders
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.compiled.ClassFileDecompiler
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.impl.compiled.ClsDecompilerImpl
import com.intellij.psi.stubs.BinaryFileStubBuilders
import org.jetbrains.kotlin.analysis.decompiler.konan.K2KotlinNativeMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetaFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.analysis.test.framework.services.disposableProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiLibraryBaseTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        val applicationDisposable = testServices.disposableProvider.getApplicationDisposable()
        for (fileType in listOf(JavaClassFileType.INSTANCE, KotlinBuiltInFileType, KlibMetaFileType)) {
            FileTypeFileViewProviders.INSTANCE.addExplicitExtension(
                fileType,
                ClassFileViewProviderFactory(),
                applicationDisposable,
            )

            BinaryFileStubBuilders.INSTANCE.addExplicitExtension(fileType, ClassFileStubBuilder(), applicationDisposable)
            BinaryFileTypeDecompilers.getInstance().addExplicitExtension(fileType, ClassFileDecompiler(), applicationDisposable)
        }

        ClassFileDecompilers.getInstance().EP_NAME.point.apply {
            registerExtension(KotlinClassFileDecompiler(), LoadingOrder.FIRST, applicationDisposable)
            registerExtension(KotlinBuiltInDecompiler(), LoadingOrder.FIRST, applicationDisposable)
            registerExtension(
                K2KotlinNativeMetadataDecompiler(),
                LoadingOrder.FIRST,
                applicationDisposable,
            )

            registerExtension(ClsDecompilerImpl(), LoadingOrder.FIRST, applicationDisposable)
        }
    }
}

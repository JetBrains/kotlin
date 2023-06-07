/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.psi.ClassFileViewProviderFactory
import com.intellij.psi.FileTypeFileViewProviders
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.analysis.test.framework.services.disposableProvider
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.FileAttributeService
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiLibraryBaseTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {
    }

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
    }

    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {
    }

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        FileTypeFileViewProviders.INSTANCE.addExplicitExtension(JavaClassFileType.INSTANCE, ClassFileViewProviderFactory())

        ClassFileDecompilers.getInstance().EP_NAME.point.apply {
            registerExtension(KotlinClassFileDecompiler(), LoadingOrder.FIRST, testServices.disposableProvider.getApplicationDisposable())
            registerExtension(KotlinBuiltInDecompiler(), LoadingOrder.FIRST, testServices.disposableProvider.getApplicationDisposable())
        }

    }
}
/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.configurators

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.psi.ClassFileViewProviderFactory
import com.intellij.psi.FileTypeFileViewProviders
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentConfigurator

abstract class AnalysisApiLibraryBaseTestApplicationEnvironmentConfiguration : AnalysisApiBaseTestApplicationEnvironmentConfiguration() {
    init {
        addConfigurator(AnalysisApiLibraryBaseTestApplicationEnvironmentConfigurator)
    }
}

object DefaultAnalysisApiLibraryTestApplicationEnvironmentConfiguration : AnalysisApiLibraryBaseTestApplicationEnvironmentConfiguration()

private object AnalysisApiLibraryBaseTestApplicationEnvironmentConfigurator : KotlinCoreApplicationEnvironmentConfigurator {
    override fun configure(applicationEnvironment: KotlinCoreApplicationEnvironment) {
        FileTypeFileViewProviders.INSTANCE.addExplicitExtension(JavaClassFileType.INSTANCE, ClassFileViewProviderFactory())

        ClassFileDecompilers.getInstance().EP_NAME.point.apply {
            registerExtension(KotlinClassFileDecompiler(), LoadingOrder.FIRST, applicationEnvironment.parentDisposable)
            registerExtension(KotlinBuiltInDecompiler(), LoadingOrder.FIRST, applicationEnvironment.parentDisposable)
        }
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.allopen

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.AbstractFirDiagnosticsTest
import org.jetbrains.kotlin.fir.extensions.FirExtensionPointService
import org.jetbrains.kotlin.fir.extensions.registerExtensions
import java.io.File

abstract class AbstractFirAllOpenDiagnosticTest : AbstractFirDiagnosticsTest() {
    override fun registerFirExtensions(service: FirExtensionPointService) {
        service.registerExtensions(FirAllOpenComponentRegistrar().configure())
    }

    override fun performCustomConfiguration(configuration: CompilerConfiguration) {
        super.performCustomConfiguration(configuration)
        configuration.addJvmClasspathRoot(File("compiler/fir/plugins/allopen-plugin/allopen-annotations/build/libs/allopen-annotations-1.4.255-SNAPSHOT.jar"))
    }
}
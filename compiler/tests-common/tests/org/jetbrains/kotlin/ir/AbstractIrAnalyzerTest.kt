/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTestWithStdLib
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.resolve.BindingContext
import java.io.File

abstract class AbstractIrAnalyzerTest: AbstractDiagnosticsTestWithStdLib() {
    override fun performAdditionalChecksAfterDiagnostics(
        testDataFile: File,
        testFiles: List<TestFile>,
        moduleFiles: Map<TestModule?, List<TestFile>>,
        moduleDescriptors: Map<TestModule?, ModuleDescriptorImpl>,
        moduleBindings: Map<TestModule?, BindingContext>
    ) {
        val actualIrDump = StringBuilder()
        for (module in moduleFiles.keys) {
            val ktFiles = moduleFiles[module]?.mapNotNull { it.ktFile } ?: continue
            val moduleDescriptor = moduleDescriptors[module] ?: continue
            val moduleBindingContext = moduleBindings[module] ?: continue
            val irModule = AbstractIrGeneratorTestCase.generateIrModule(ktFiles, moduleDescriptor, moduleBindingContext, true)
            // here we have BindingContext and IrTree
            println("wow")
        }
    }
}
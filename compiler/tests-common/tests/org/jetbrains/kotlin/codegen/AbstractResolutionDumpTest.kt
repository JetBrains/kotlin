/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.jvm.compiler.tmp.IrBasedResolutionDumpBuilder
import org.jetbrains.kotlin.cli.jvm.compiler.tmp.impls.TextBasedNativeElementsFactory
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.ir.AbstractIrGeneratorTestCase
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import java.io.File

class AbstractResolutionDumpTest : AbstractIrGeneratorTestCase() {
    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        val (bindingContext, moduleDescriptor, _) = JvmResolveUtil.analyze(myFiles.psiFiles, myEnvironment)
        val generatorContext = GeneratorContext(Psi2IrConfiguration(ignoreErrors = true), moduleDescriptor, bindingContext)
        val psi2ir = Psi2IrTranslator(LanguageVersionSettingsImpl.DEFAULT)
        val builder = IrBasedResolutionDumpBuilder(bindingContext, TextBasedNativeElementsFactory, File(""))

        val moduleFragment = psi2ir.generateModuleFragment(generatorContext, myFiles.psiFiles)

        for (irFile in moduleFragment.files) {
            val physicalFile = File(irFile.fileEntry.name)
            builder.buildResolutionDumpForFile(irFile, physicalFile)
        }
    }
}
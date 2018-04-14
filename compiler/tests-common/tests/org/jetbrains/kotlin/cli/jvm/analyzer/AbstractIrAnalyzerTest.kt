/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.analyzer

import junit.framework.TestCase
import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTestWithStdLib
import org.jetbrains.kotlin.checkers.LazyOperationsLog
import org.jetbrains.kotlin.checkers.LoggingStorageManager
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.AbstractIrGeneratorTestCase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import java.io.File
import java.util.*

abstract class AbstractIrAnalyzerTest : AbstractDiagnosticsTestWithStdLib() {
    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        val groupedByModule = files.groupBy(TestFile::module)

        val lazyOperationsLog: LazyOperationsLog?

        val tracker = ExceptionTracker()
        val storageManager: StorageManager
        if (files.any(TestFile::checkLazyLog)) {
            lazyOperationsLog = LazyOperationsLog(AbstractDiagnosticsTest.HASH_SANITIZER)
            storageManager = LoggingStorageManager(
                LockBasedStorageManager.createWithExceptionHandling(tracker),
                lazyOperationsLog.addRecordFunction
            )
        } else {
            storageManager = LockBasedStorageManager.createWithExceptionHandling(tracker)
        }

        val context = SimpleGlobalContext(storageManager, tracker)

        val modules = createModules(groupedByModule, context.storageManager)
        val moduleBindings = HashMap<TestModule?, BindingContext>()

        for ((testModule, testFilesInModule) in groupedByModule) {
            val ktFiles = getKtFiles(testFilesInModule, true)

            val oldModule = modules[testModule]!!

            val languageVersionSettings = loadLanguageVersionSettings(testFilesInModule)
            val moduleContext = context.withProject(project).withModule(oldModule)

            val separateModules = groupedByModule.size == 1 && groupedByModule.keys.single() == null
            val result = analyzeModuleContents(
                moduleContext, ktFiles, NoScopeRecordCliBindingTrace(),
                languageVersionSettings, separateModules, loadJvmTarget(testFilesInModule)
            )
            if (oldModule != result.moduleDescriptor) {
                modules[testModule] = result.moduleDescriptor as ModuleDescriptorImpl
                for (module in modules.values) {
                    @Suppress("DEPRECATION")
                    val it = (module.testOnly_AllDependentModules as MutableList).listIterator()
                    while (it.hasNext()) {
                        if (it.next() == oldModule) {
                            it.set(result.moduleDescriptor as ModuleDescriptorImpl)
                        }
                    }
                }
            }

            moduleBindings[testModule] = result.bindingContext
        }

        extractStructuresForModules(testDataFile.nameWithoutExtension, groupedByModule, modules, moduleBindings)
    }

    private fun extractStructuresForModules(
        filename: String,
        moduleFiles: Map<TestModule?, List<TestFile>>,
        moduleDescriptors: Map<TestModule?, ModuleDescriptorImpl>,
        moduleBindings: Map<TestModule?, BindingContext>
    ) {
        for (module in moduleFiles.keys) {
            val ktFiles = moduleFiles[module]?.mapNotNull { it.ktFile } ?: continue
            val moduleDescriptor = moduleDescriptors[module] ?: continue
            val moduleBindingContext = moduleBindings[module] ?: continue
            val irModule = AbstractIrGeneratorTestCase.generateIrModule(ktFiles, moduleDescriptor, moduleBindingContext, true)

            checkOneSource(filename, irModule, moduleDescriptor, moduleBindingContext)
        }
    }

    private fun checkOneSource(
        filename: String,
        irModule: IrModuleFragment,
        moduleDescriptor: ModuleDescriptor,
        bindingContext: BindingContext
    ) {
        val data = analyzers.get(filename)
        if (data != null) {
            val (analyzer, expected) = data
            println("------------${analyzer.title}------------")
            analyzer.execute(irModule, moduleDescriptor, bindingContext)
            println("------------")
            println()
        } else {
            TestCase.fail("analyzer \"$filename\" not founded")
        }
    }

}

fun foo() {}
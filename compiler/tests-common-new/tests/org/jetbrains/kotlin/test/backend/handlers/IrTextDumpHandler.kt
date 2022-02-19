/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpTreesFromLineNumber
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_EXTERNAL_CLASS
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.EXTERNAL_FILE
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_IDENTICAL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension
import java.io.File

class IrTextDumpHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    companion object {
        const val DUMP_EXTENSION = "ir.txt"

        fun computeDumpExtension(module: TestModule, defaultExtension: String): String {
            return if (module.frontendKind == FrontendKinds.ClassicFrontend || FIR_IDENTICAL in module.directives)
                defaultExtension else "fir.$defaultExtension"
        }

        fun List<IrFile>.groupWithTestFiles(module: TestModule): List<Pair<TestFile?, IrFile>> = mapNotNull { irFile ->
            val name = File(irFile.fileEntry.name).name
            val testFile = module.files.firstOrNull { it.name == name }
            testFile to irFile
        }
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives, FirDiagnosticsDirectives)

    private val baseDumper = MultiModuleInfoDumper()
    private val buildersForSeparateFileDumps: MutableMap<File, StringBuilder> = mutableMapOf()

    @OptIn(ExperimentalStdlibApi::class)
    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (DUMP_IR !in module.directives) return
        val irFiles = info.irModuleFragment.files
        val testFileToIrFile = irFiles.groupWithTestFiles(module)
        val builder = baseDumper.builderForModule(module)
        for ((testFile, irFile) in testFileToIrFile) {
            if (testFile?.directives?.contains(EXTERNAL_FILE) == true) continue
            var actualDump = irFile.dumpTreesFromLineNumber(lineNumber = 0, normalizeNames = true)
            if (actualDump.isEmpty()) {
                actualDump = irFile.dumpTreesFromLineNumber(lineNumber = UNDEFINED_OFFSET, normalizeNames = true)
            }
            builder.append(actualDump)
        }
        compareDumpsOfExternalClasses(module, info)
    }

    private fun compareDumpsOfExternalClasses(module: TestModule, info: IrBackendInput) {
        // FIR doesn't support searching descriptors
        if (module.frontendKind == FrontendKinds.FIR) return

        val externalClassFqns = module.directives[DUMP_EXTERNAL_CLASS]
        if (externalClassFqns.isEmpty()) return

        // TODO: why JS one is used here in original AbstractIrTextTestCase?
        val mangler = JsManglerDesc
        val signaturer = IdSignatureDescriptor(mangler)
        val irModule = info.irModuleFragment
        val stubGenerator = DeclarationStubGeneratorImpl(
            irModule.descriptor,
            SymbolTable(signaturer, IrFactoryImpl), // TODO
            irModule.irBuiltins,
            DescriptorByIdSignatureFinderImpl(irModule.descriptor, mangler)
        )

        val baseFile = testServices.moduleStructure.originalTestDataFiles.first()
        for (externalClassFqn in externalClassFqns) {
            val classDump = stubGenerator.generateExternalClass(irModule.descriptor, externalClassFqn).dump()
            val expectedFile = baseFile.withSuffixAndExtension("__$externalClassFqn", module.dumpExtension)
            assertions.assertEqualsToFile(expectedFile, classDump)
        }
    }

    private fun DeclarationStubGenerator.generateExternalClass(descriptor: ModuleDescriptor, externalClassFqn: String): IrClass {
        val classDescriptor =
            descriptor.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(externalClassFqn)))
                ?: throw AssertionError("Can't find a class in external dependencies: $externalClassFqn")

        return generateMemberStub(classDescriptor) as IrClass
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val moduleStructure = testServices.moduleStructure
        val defaultExpectedFile = moduleStructure.originalTestDataFiles.first().withExtension(moduleStructure.modules.first().dumpExtension)
        checkOneExpectedFile(defaultExpectedFile, baseDumper.generateResultingDump())
        buildersForSeparateFileDumps.entries.forEach { (expectedFile, dump) -> checkOneExpectedFile(expectedFile, dump.toString()) }
    }

    private fun checkOneExpectedFile(expectedFile: File, actualDump: String) {
        if (actualDump.isNotEmpty()) {
            assertions.assertEqualsToFile(expectedFile, actualDump)
        }
    }

    private val TestModule.dumpExtension: String
        get() = computeDumpExtension(this, DUMP_EXTENSION)
}

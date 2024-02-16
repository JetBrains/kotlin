/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.allOverridden
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpTreesFromLineNumber
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_EXTERNAL_CLASS
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.EXTERNAL_FILE
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_IDENTICAL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension
import java.io.File

class IrTextDumpHandler(
    testServices: TestServices,
    artifactKind: BackendKind<IrBackendInput>,
) : AbstractIrHandler(testServices, artifactKind) {
    companion object {
        const val DUMP_EXTENSION = "ir.txt"

        fun computeDumpExtension(module: TestModule, defaultExtension: String, ignoreFirIdentical: Boolean = false): String {
            return if (
                module.frontendKind == FrontendKinds.ClassicFrontend ||
                (!ignoreFirIdentical && FIR_IDENTICAL in module.directives)
            ) {
                defaultExtension
            } else {
                "fir.$defaultExtension"
            }
        }

        fun List<IrFile>.groupWithTestFiles(module: TestModule): List<Pair<TestFile?, IrFile>> = mapNotNull { irFile ->
            val name = File(irFile.fileEntry.name).name
            val testFile = module.files.firstOrNull { it.name == name }
            testFile to irFile
        }

        private val HIDDEN_ENUM_METHOD_NAMES = setOf(
            Name.identifier("finalize"), // JVM-specific fake override from java.lang.Enum. TODO: remove it after fixing KT-63744
            Name.identifier("getDeclaringClass"), // JVM-specific fake override from java.lang.Enum. TODO: remove it after fixing KT-63744
            Name.identifier("clone"), // JVM-specific fake override from kotlin.Enum (not java.lang.Enum !).
        )

        private fun IrSimpleFunction.isHiddenEnumMethod(irBuiltIns: IrBuiltIns): Boolean {
            return isFakeOverride && allOverridden(includeSelf = true).any {
                it.dispatchReceiverParameter?.type?.classOrNull == irBuiltIns.enumClass && it.name in HIDDEN_ENUM_METHOD_NAMES
            }
        }

        fun isHiddenDeclaration(declaration: IrDeclaration, irBuiltIns: IrBuiltIns): Boolean =
            (declaration as? IrSimpleFunction)?.isHiddenEnumMethod(irBuiltIns) == true
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives, FirDiagnosticsDirectives)

    override val additionalAfterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
        get() = listOf(::FirIrDumpIdenticalChecker)

    private val baseDumper = MultiModuleInfoDumper()
    private val buildersForSeparateFileDumps: MutableMap<File, StringBuilder> = mutableMapOf()

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (DUMP_IR !in module.directives) return

        val irBuiltins = info.irModuleFragment.irBuiltins

        val dumpOptions = DumpIrTreeOptions(
            normalizeNames = true,
            printFacadeClassInFqNames = false,
            printFlagsInDeclarationReferences = false,
            // KT-60248 Abbreviations should not be rendered to make K2 IR dumps closer to K1 IR dumps during irText tests.
            // PSI2IR assigns field `abbreviation` with type abbreviation. It serves only debugging purposes, and no compiler functionality relies on it.
            // FIR2IR does not initialize field `abbreviation` at all.
            printTypeAbbreviations = false,
            isHiddenDeclaration = { isHiddenDeclaration(it, irBuiltins) },
            stableOrder = true,
        )

        val builder = baseDumper.builderForModule(module.name)
        val testFileToIrFile = info.irModuleFragment.files.groupWithTestFiles(module)
        for ((testFile, irFile) in testFileToIrFile) {
            if (testFile?.directives?.contains(EXTERNAL_FILE) == true) continue
            var actualDump = irFile.dumpTreesFromLineNumber(lineNumber = 0, dumpOptions)
            if (actualDump.isEmpty()) {
                actualDump = irFile.dumpTreesFromLineNumber(lineNumber = UNDEFINED_OFFSET, dumpOptions)
            }
            builder.append(actualDump)
        }

        compareDumpsOfExternalClasses(module, info)
    }

    private fun compareDumpsOfExternalClasses(module: TestModule, info: IrBackendInput) {
        val externalClassIds = module.directives[DUMP_EXTERNAL_CLASS]
        if (externalClassIds.isEmpty()) return

        val baseFile = testServices.moduleStructure.originalTestDataFiles.first()
        assertions.assertAll(
            externalClassIds.map { externalClassId ->
                {
                    val classDump = info.irPluginContext.findExternalClass(externalClassId).dump()
                    val suffix = ".__${externalClassId.replace("/", ".")}"
                    val expectedFile = baseFile.withSuffixAndExtension(suffix, module.getDumpExtension(ignoreFirIdentical = true))
                    assertions.assertEqualsToFile(expectedFile, classDump)
                }
            }
        )
    }

    private fun IrPluginContext.findExternalClass(externalClassId: String): IrClass {
        val classId = ClassId.fromString(externalClassId)
        return referenceClass(classId)?.owner ?: assertions.fail { "Can't find a class in external dependencies: $externalClassId" }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val moduleStructure = testServices.moduleStructure
        val defaultExpectedFile = moduleStructure.originalTestDataFiles.first()
            .withExtension(moduleStructure.modules.first().getDumpExtension())
        checkOneExpectedFile(defaultExpectedFile, baseDumper.generateResultingDump())
        buildersForSeparateFileDumps.entries.forEach { (expectedFile, dump) -> checkOneExpectedFile(expectedFile, dump.toString()) }
    }

    private fun checkOneExpectedFile(expectedFile: File, actualDump: String) {
        if (actualDump.isNotEmpty()) {
            assertions.assertEqualsToFile(expectedFile, actualDump)
        }
    }

    private fun TestModule.getDumpExtension(ignoreFirIdentical: Boolean = false): String {
        return computeDumpExtension(this, DUMP_EXTENSION, ignoreFirIdentical)
    }
}


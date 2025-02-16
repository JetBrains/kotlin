/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions
import org.jetbrains.kotlin.ir.util.DumpIrTreeOptions.FlagsFilter
import org.jetbrains.kotlin.ir.util.allOverridden
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpTreesFromLineNumber
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.CHECK_BYTECODE_LISTING
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_EXTERNAL_CLASS
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.EXTERNAL_FILE
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_IDENTICAL
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.independentSourceDirectoryPath
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import java.io.File

class IrTextDumpHandler(
    testServices: TestServices,
    artifactKind: BackendKind<IrBackendInput>,
) : AbstractIrHandler(testServices, artifactKind) {
    companion object {
        const val DUMP_EXTENSION = "ir.txt"
        const val DUMP_EXTENSION2 = "ir2.txt"

        fun computeDumpExtension(
            testServices: TestServices,
            defaultExtension: String,
            ignoreFirIdentical: Boolean = false,
        ): String {
            return if (
                testServices.defaultsProvider.frontendKind == FrontendKinds.ClassicFrontend ||
                (!ignoreFirIdentical && FIR_IDENTICAL in testServices.moduleStructure.allDirectives)
            ) {
                defaultExtension
            } else {
                "fir.$defaultExtension"
            }
        }

        fun List<IrFile>.groupWithTestFiles(testServices: TestServices, ordered: Boolean = false): List<Pair<Pair<TestModule, TestFile>?, IrFile>> {
            return mapNotNull { irFile ->
                val name = File(irFile.fileEntry.name).name
                val moduleAndFile = testServices.moduleStructure.modules.firstNotNullOfOrNull { module ->
                    val file = module.files.firstOrNull { it.name == name } ?: return@firstNotNullOfOrNull null
                    module to file
                }
                moduleAndFile to irFile
            }.applyIf(ordered) {
                sortedBy { (moduleAndFile, irFile) ->
                    val pathFromIrFile = irFile.fileEntry.name
                    val (module, _) = moduleAndFile ?: return@sortedBy pathFromIrFile
                    pathFromIrFile.removePrefix(module.independentSourceDirectoryPath(testServices))
                }
            }
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

        fun renderFilePathForIrFile(
            testFileToIrFile: List<Pair<Pair<TestModule, TestFile>?, IrFile>>,
            testServices: TestServices,
            irFile: IrFile,
            fullPath: String,
        ): String {
            val (correspondingModule, _) = testFileToIrFile.firstOrNull { it.second == irFile }?.first ?: return fullPath
            return fullPath.removePrefix(correspondingModule.independentSourceDirectoryPath(testServices))
        }
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CodegenTestDirectives, FirDiagnosticsDirectives)

    override val additionalAfterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
        get() = listOf(::FirIrDumpIdenticalChecker)

    private val baseDumper = MultiModuleInfoDumper()
    private val buildersForSeparateFileDumps: MutableMap<File, StringBuilder> = mutableMapOf()

    private var byteCodeListingEnabled = false

    override fun processModule(module: TestModule, info: IrBackendInput) {
        byteCodeListingEnabled = byteCodeListingEnabled || CHECK_BYTECODE_LISTING in module.directives

        if (DUMP_IR !in module.directives) return

        val testFileToIrFile = info.irModuleFragment.files.groupWithTestFiles(testServices, ordered = true)
        val dumpOptions = DumpIrTreeOptions(
            normalizeNames = true,
            printFacadeClassInFqNames = false,
            declarationFlagsFilter = FlagsFilter { declaration, isReference, flags ->
                // By coincidence, there is a huge number of cases in IR text test data files
                // when flags are still rendered for references to fields and classes.
                flags.takeIf { !isReference || declaration is IrField || declaration is IrClass }.orEmpty()
            },
            // KT-60248 Abbreviations should not be rendered to make K2 IR dumps closer to K1 IR dumps during irText tests.
            // PSI2IR assigns field `abbreviation` with type abbreviation. It serves only debugging purposes, and no compiler functionality relies on it.
            // FIR2IR does not initialize field `abbreviation` at all.
            printTypeAbbreviations = false,
            isHiddenDeclaration = { isHiddenDeclaration(it, info.irPluginContext.irBuiltIns) },
            stableOrder = true,
            filePathRenderer = { irFile, fullPath ->
                renderFilePathForIrFile(testFileToIrFile, testServices, irFile, fullPath)
            }
        )
        val builder = baseDumper.builderForModule(module.name)

        for ((moduleAndFile, irFile) in testFileToIrFile) {
            if (moduleAndFile?.second?.directives?.contains(EXTERNAL_FILE) == true) continue
            val actualDump = irFile.dumpTreesFromLineNumber(lineNumber = 0, dumpOptions)
            builder.append(actualDump)
        }

        compareDumpsOfExternalClasses(module, info)
    }

    private fun compareDumpsOfExternalClasses(module: TestModule, info: IrBackendInput) {
        val externalClassIds = module.directives[DUMP_EXTERNAL_CLASS]
        if (externalClassIds.isEmpty()) return
        val dumpOptions = DumpIrTreeOptions(stableOrder = true, printFilePath = false)
        val baseFile = testServices.moduleStructure.originalTestDataFiles.first()
        assertions.assertAll(
            externalClassIds.map { externalClassId ->
                {
                    val classDump = info.irPluginContext.findExternalClass(externalClassId).dump(dumpOptions)
                    val suffix = ".__${externalClassId.replace("/", ".")}"
                    val expectedFile = baseFile.withSuffixAndExtension(suffix, getDumpExtension(ignoreFirIdentical = true))
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
            .withExtension(getDumpExtension())
        checkOneExpectedFile(defaultExpectedFile, baseDumper.generateResultingDump())
        buildersForSeparateFileDumps.entries.forEach { (expectedFile, dump) -> checkOneExpectedFile(expectedFile, dump.toString()) }
    }

    private fun checkOneExpectedFile(expectedFile: File, actualDump: String) {
        if (actualDump.isNotEmpty()) {
            assertions.assertEqualsToFile(expectedFile, actualDump)
        } else {
            assertions.assertFileDoesntExist(expectedFile, DUMP_IR)
        }
    }

    private fun getDumpExtension(ignoreFirIdentical: Boolean = false): String {
        return computeDumpExtension(testServices, if (byteCodeListingEnabled) DUMP_EXTENSION2 else DUMP_EXTENSION, ignoreFirIdentical)
    }
}


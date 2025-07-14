/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.google.common.collect.Sets
import org.jetbrains.kotlin.abicmp.defects.Location
import org.jetbrains.kotlin.abicmp.reports.*
import org.jetbrains.kotlin.abicmp.tag
import org.jetbrains.kotlin.abicmp.tasks.ClassTask
import org.jetbrains.kotlin.abicmp.tasks.ModuleMetadataTask
import org.jetbrains.kotlin.abicmp.tasks.checkerConfiguration
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.codegen.getKotlinModuleFile
import org.jetbrains.kotlin.test.backend.ir.AbiCheckerSuppressor
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter

class JvmAbiConsistencyHandler(testServices: TestServices) : AnalysisHandler<BinaryArtifacts.JvmFromK1AndK2>(testServices, true, true) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.JvmFromK1AndK2>
        get() = ArtifactKinds.JvmFromK1AndK2


    private class ModuleReport(
        val missingInK1: Set<String>,
        val missingInK2: Set<String>,
        val nonEmptyClassReports: MutableList<ClassReport>,
        val moduleMetadataReport: ModuleMetadataReport,
    )

    private class TestReport {
        private val modulesWithNonEmptyReports = mutableMapOf<String, ModuleReport>()

        fun addModuleReport(moduleName: String, report: ModuleReport) {
            modulesWithNonEmptyReports[moduleName] = report
        }

        fun isEmpty(): Boolean = modulesWithNonEmptyReports.isEmpty()

        fun dumpAsPlainText(): String {

            fun TextTreeBuilderContext.reportMissing(frontendName: String, missing: Set<String>) {
                if (missing.isEmpty()) return
                node("Missing in $frontendName") {
                    node(missing.joinToString("\n"))
                }
            }

            val outputStream = ByteArrayOutputStream()
            val out = PrintWriter(outputStream)

            dumpTree(out) {
                modulesWithNonEmptyReports.forEach { (module, report) ->
                    node("MODULE $module") {
                        reportMissing("K1", report.missingInK1)
                        reportMissing("K2", report.missingInK2)

                        report.nonEmptyClassReports.forEach { classReport ->
                            with(classReport) { appendClassReport() }
                        }

                        if (!report.moduleMetadataReport.isEmpty()) {
                            with(report.moduleMetadataReport) { appendModuleMetadataReport() }
                        }
                    }
                }
            }

            return outputStream.toString()
        }

        // Might be used while debugging
        @Suppress("unused")
        fun dumpAsHtml(): String {
            fun PrintWriter.reportMissing(frontendName: String, missing: Set<String>) {
                if (missing.isEmpty()) return
                println("Missing in $frontendName:")
                tag("br")
                println(missing.joinToString())
                tag("br")
                tag("br")
            }

            val outputStream = ByteArrayOutputStream()
            PrintWriter(outputStream, true).use { out ->
                out.tag("html") {
                    out.tag("head") {
                        out.tag("style", REPORT_CSS)
                    }
                    out.tag("body") {
                        modulesWithNonEmptyReports.forEach { (module, report) ->
                            out.tag("h2", "Module: $module")
                            out.reportMissing("K1", report.missingInK1)
                            out.reportMissing("K2", report.missingInK2)
                            report.nonEmptyClassReports.forEach { it.writeAsHtml(out) }
                        }

                    }
                }
            }
            return outputStream.toString()
        }
    }

    private val testReport = TestReport()

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val txtDiffPath = testServices.moduleStructure.originalTestDataFiles.first().withExtension(".jvm_abi.txt")
        val isDifferenceExplained = testServices.moduleStructure.allDirectives.contains(CodegenTestDirectives.JVM_ABI_K1_K2_DIFF)

        if (!testReport.isEmpty()) {
            JUnit5Assertions.assertEqualsToFile(
                txtDiffPath,
                testReport.dumpAsPlainText(),
                sanitizer = { it },
                differenceObtainedMessage = { "Actual K1/K2 JVM ABI difference differs from expected" },
                fileNotFoundMessageTeamCity = fileNotFoundMessageBuilder(isTeamCityVersion = true, isDifferenceExplained),
                fileNotFoundMessageLocal = fileNotFoundMessageBuilder(isTeamCityVersion = false, isDifferenceExplained)
            )

            if (!isDifferenceExplained) {
                fail(
                    "K1/K2 JVM ABI difference obtained. Add ${CodegenTestDirectives.JVM_ABI_K1_K2_DIFF.name} directive with an explanation"
                )
            }
        } else {
            if (isDifferenceExplained && txtDiffPath.exists()) {
                fail(
                    "No K1/K2 JVM ABI difference found. Remove ${CodegenTestDirectives.JVM_ABI_K1_K2_DIFF.name} directive and $txtDiffPath"
                )
            }
            if (isDifferenceExplained) {
                fail("No K1/K2 JVM ABI difference found. Remove ${CodegenTestDirectives.JVM_ABI_K1_K2_DIFF.name} directive")
            }
            if (txtDiffPath.exists()) {
                fail("No K1/K2 JVM ABI difference found. Remove $txtDiffPath")
            }
        }
    }

    private fun fileNotFoundMessageBuilder(isTeamCityVersion: Boolean, isDiffExplained: Boolean): ((File) -> String) = {
        buildString {
            if (isTeamCityVersion) {
                append("Expected data file did not exist `$it`")
            } else {
                append("Expected data file did not exist. Generating: $it")
            }
            if (!isDiffExplained) {
                append("\n")
                append("Also add ${CodegenTestDirectives.JVM_ABI_K1_K2_DIFF.name} directive with an explanation")
            }
        }
    }

    private fun fail(message: String) {
        JUnit5Assertions.fail { message }
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.JvmFromK1AndK2) {
        if (AbiCheckerSuppressor.ignoredByBackendOrInliner(testServices)) return
        val classesFromK1 = info.fromK1.classFileFactory.getClassFiles().associate { it.relativePath to it.asByteArray() }
        val classesFromK2 = info.fromK2.classFileFactory.getClassFiles().associate { it.relativePath to it.asByteArray() }
        val missingInK2 = Sets.difference(classesFromK1.keys, classesFromK2.keys).toMutableSet()
        val missingInK1 = Sets.difference(classesFromK2.keys, classesFromK1.keys).toMutableSet()
        val commonClasses = Sets.intersection(classesFromK1.keys, classesFromK2.keys)

        val configuration = checkerConfiguration {
            // Indication of constants is different in K2 as expected
            // We simply turn the checker off to avoid producing an enormous number of difference reports
            disable("class.metadata.property.hasConstant")
        }

        val nonEmptyClassReports = mutableListOf<ClassReport>()
        commonClasses.forEach { classInternalName ->
            val k1ClassNode = parseClassNode(classesFromK1[classInternalName]!!)
            val k2ClassNode = parseClassNode(classesFromK2[classInternalName]!!)
            val classReport = ClassReport(Location.Class("", classInternalName), classInternalName, "K1", "K2", DefectReport())
            ClassTask(configuration, k1ClassNode, k2ClassNode, classReport).run()
            if (classReport.isNotEmpty()) {
                nonEmptyClassReports.add(classReport)
            }
        }

        val k1ModuleFile = info.fromK1.classFileFactory.getKotlinModuleFile()
        val k2ModuleFile = info.fromK2.classFileFactory.getKotlinModuleFile()

        if (k1ModuleFile == null && k2ModuleFile != null) {
            missingInK1.add(k2ModuleFile.relativePath)
        }

        if (k1ModuleFile != null && k2ModuleFile == null) {
            missingInK2.add(k1ModuleFile.relativePath)
        }

        val moduleMetadataReport = ModuleMetadataReport("K1", "K2")
        if (k1ModuleFile != null && k2ModuleFile != null) {
            ModuleMetadataTask(configuration, k1ModuleFile.asByteArray(), k2ModuleFile.asByteArray(), moduleMetadataReport).run()
        }

        if (nonEmptyClassReports.isNotEmpty() || missingInK1.isNotEmpty() || missingInK2.isNotEmpty() || !moduleMetadataReport.isEmpty()) {
            testReport.addModuleReport(module.name, ModuleReport(missingInK1, missingInK2, nonEmptyClassReports, moduleMetadataReport))
        }
    }

    private fun parseClassNode(byteArray: ByteArray) =
        ClassNode().also { ClassReader(ByteArrayInputStream(byteArray)).accept(it, ClassReader.SKIP_CODE) }
}
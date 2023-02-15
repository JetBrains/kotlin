/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.isJavaFile
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.util.DescriptorValidator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.utils.keysToMap
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Compares dump of descriptors if with expected
 * Dump lays in file testName.txt
 * If there is no .txt file than handler does nothing
 */
class DeclarationsDumpHandler(
    testServices: TestServices
) : ClassicFrontendAnalysisHandler(testServices) {
    companion object {
        private val NAMES_OF_CHECK_TYPE_HELPER = listOf("checkSubtype", "CheckTypeInv", "_", "checkType").map { Name.identifier(it) }

        private val JAVA_PACKAGE_PATTERN = Pattern.compile("^\\s*package [.\\w\\d]*", Pattern.MULTILINE)
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(DiagnosticsDirectives)

    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper(moduleHeaderTemplate = "// -- Module: <%s> --")

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val resultDump = dumper.generateResultingDump()
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val allDirectives = testServices.moduleStructure.allDirectives
        val prefix = when {
            DiagnosticsDirectives.NI_EXPECTED_FILE in allDirectives &&
                    testServices.moduleStructure.modules.any { it.languageVersionSettings.supportsFeature(LanguageFeature.NewInference) } -> ".ni"

            JvmEnvironmentConfigurationDirectives.USE_JAVAC in allDirectives
                    && DiagnosticsDirectives.JAVAC_EXPECTED_FILE in allDirectives -> ".javac"

            else -> ""
        }
        val expectedFileName = "${testDataFile.nameWithoutExtension}$prefix.txt"
        val expectedFile = testDataFile.parentFile.resolve(expectedFileName)
        if (!expectedFile.exists()) return
        assertions.assertEqualsToFile(expectedFile, resultDump)
    }

    override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
        if (DiagnosticsDirectives.SKIP_TXT in module.directives) return
        val moduleDescriptor = info.analysisResult.moduleDescriptor
        val checkTypeEnabled = AdditionalFilesDirectives.CHECK_TYPE in module.directives
        val comparator = RecursiveDescriptorComparator(
            createdAffectedPackagesConfiguration(module.files, info.ktFiles, moduleDescriptor, checkTypeEnabled)
        )
        val packages = buildList {
            module.directives[DiagnosticsDirectives.RENDER_PACKAGE].forEach {
                add(FqName(it))
            }
            add(FqName.ROOT)
        }
        val textByPackage = packages.keysToMap { StringBuilder() }

        for ((packageName, packageText) in textByPackage.entries) {
            val aPackage = moduleDescriptor.getPackage(packageName)
            assertions.assertFalse(aPackage.isEmpty())

            val actualSerialized = comparator.serializeRecursively(aPackage)
            packageText.append(actualSerialized)
        }
        val allPackagesText = textByPackage.values.joinToString("\n")
        dumper.builderForModule(module).appendLine(allPackagesText)
    }

    private fun createdAffectedPackagesConfiguration(
        testFiles: List<TestFile>,
        ktFiles: Map<TestFile, KtFile>,
        moduleDescriptor: ModuleDescriptor,
        checkTypeEnabled: Boolean
    ): RecursiveDescriptorComparator.Configuration {
        val packagesNames = testFiles.mapNotNullTo(mutableSetOf()) {
            val ktFile = ktFiles[it]
            when {
                ktFile != null -> ktFile.packageFqName.pathSegments().firstOrNull() ?: SpecialNames.ROOT_PACKAGE
                it.isJavaFile -> getJavaFilePackage(it)
                else -> null
            }
        }
        val stepIntoFilter = Predicate<DeclarationDescriptor> { descriptor ->
            val module = DescriptorUtils.getContainingModuleOrNull(descriptor)
            if (module != moduleDescriptor) return@Predicate false

            if (descriptor is PackageViewDescriptor) {
                val fqName = descriptor.fqName
                return@Predicate fqName.isRoot || fqName.pathSegments().first() in packagesNames
            }

            if (checkTypeEnabled && descriptor.name in NAMES_OF_CHECK_TYPE_HELPER) return@Predicate false

            true
        }
        return RECURSIVE.filterRecursion(stepIntoFilter)
            .withValidationStrategy(DescriptorValidator.ValidationVisitor.errorTypesAllowed())
            .checkFunctionContracts(true)
    }

    private fun getJavaFilePackage(testFile: TestFile): Name {
        val matcher = JAVA_PACKAGE_PATTERN.matcher(testFile.originalContent)

        if (matcher.find()) {
            return testFile.originalContent
                .substring(matcher.start(), matcher.end())
                .split(" ")
                .last()
                .filter { !it.isWhitespace() }
                .let { Name.identifier(it.split(".").first()) }
        }

        return SpecialNames.ROOT_PACKAGE
    }
}

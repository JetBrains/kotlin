/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.PackageEntry
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractPerformanceImportTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected abstract fun stats(): Stats

    protected fun doPerfTest(unused: String) {
        val testName = getTestName(false)

        CodeStyle.setTemporarySettings(project, CodeStyle.getSettings(project).clone())
        val codeStyleSettings = KotlinCodeStyleSettings.getInstance(project)

        try {
            val fixture = myFixture
            val dependencySuffixes = listOf(".dependency.kt", ".dependency.java", ".dependency1.kt", ".dependency2.kt")
            for (suffix in dependencySuffixes) {
                val dependencyPath = fileName().replace(".kt", suffix)
                if (File(testDataPath, dependencyPath).exists()) {
                    fixture.configureByFile(dependencyPath)
                }
            }

            fixture.configureByFile(fileName())

            var file = fixture.file as KtFile

            var fileText = file.text

            codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT =
                InTextDirectivesUtils.getPrefixedInt(fileText, "// NAME_COUNT_TO_USE_STAR_IMPORT:") ?: nameCountToUseStarImportDefault
            codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS =
                InTextDirectivesUtils.getPrefixedInt(fileText, "// NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS:")
                    ?: nameCountToUseStarImportForMembersDefault
            codeStyleSettings.IMPORT_NESTED_CLASSES =
                InTextDirectivesUtils.getPrefixedBoolean(fileText, "// IMPORT_NESTED_CLASSES:") ?: false

            InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PACKAGE_TO_USE_STAR_IMPORTS:").forEach {
                codeStyleSettings.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(PackageEntry(false, it.trim(), false))
            }
            InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PACKAGES_TO_USE_STAR_IMPORTS:").forEach {
                codeStyleSettings.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(PackageEntry(false, it.trim(), true))
            }

            var descriptorName = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// IMPORT:")
                ?: error("No IMPORT directive defined")

            var filter: (DeclarationDescriptor) -> Boolean = { true }
            if (descriptorName.startsWith("class:")) {
                filter = { it is ClassDescriptor }
                descriptorName = descriptorName.substring("class:".length).trim()
            }

            val fqName = FqName(descriptorName)
            val importInsertHelper = ImportInsertHelper.getInstance(project)
            val psiDocumentManager = PsiDocumentManager.getInstance(project)

            performanceTest<Unit, String> {
                name(testName)
                stats(stats())
                setUp {
                    fixture.configureByFile(fileName())
                    file = fixture.file as KtFile

                    fileText = file.text
                }
                test {
                    it.value = project.executeWriteCommand<String?>("") {
                        perfTestCore(file, fqName, filter, descriptorName, importInsertHelper, psiDocumentManager)
                    }
                }
                tearDown {
                    val log = it.value
                    val testPath = testPath()
                    val afterFile = File("$testPath.after")
                    KotlinTestUtils.assertEqualsToFile(afterFile, fixture.file.text)
                    if (log != null) {
                        val logFile = File("$testPath.log")
                        if (log.isNotEmpty()) {
                            KotlinTestUtils.assertEqualsToFile(logFile, log)
                        } else {
                            TestCase.assertFalse(logFile.exists())
                        }
                    }
                    runWriteAction {
                        myFixture.file.delete()
                    }
                }
            }
        } finally {
            CodeStyle.dropTemporarySettings(project)
        }
    }

    abstract fun perfTestCore(
        file: KtFile,
        fqName: FqName,
        filter: (DeclarationDescriptor) -> Boolean,
        descriptorName: String,
        importInsertHelper: ImportInsertHelper,
        psiDocumentManager: PsiDocumentManager
    ): String?


    protected open val nameCountToUseStarImportDefault: Int
        get() = 1

    protected open val nameCountToUseStarImportForMembersDefault: Int
        get() = 3

}
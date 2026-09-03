/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.readText

abstract class AbstractMultiplatformParsing2FirConverterTestCase : AbstractRawFirBuilderTestCase() {
    @OptIn(ObsoleteTestInfrastructure::class)
    override fun runTest(filePath: String) {
        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath))
        val path = Paths.get(filePath)
        val firFile = MultiplatformParsing2Fir(
            session = FirSessionFactoryHelper.createEmptySession(parseLanguageFeatures(path.readText())),
            scopeProvider = StubFirScopeProvider,
            diagnosticsReporter = null
        ).buildFirFile(path)
        val firDump = FirRenderer.withDeclarationAttributes().renderElementAsString(firFile)

        val originalExpectedFile = File(expectedPath(filePath, ".txt"))
        val lightSyntaxTreeExpectedFile = File(expectedPath(filePath, ".lst.txt"))
        val expectedFile = lightSyntaxTreeExpectedFile.takeIf { it.exists() } ?: originalExpectedFile
        TestDataAssertions.assertEqualsToFile(expectedFile, firDump)

        if (lightSyntaxTreeExpectedFile.exists()) {
            val lightTreeFileContent = lightSyntaxTreeExpectedFile.readText().trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd()
            val originalFileContent = originalExpectedFile.readText().trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd()
            if (lightTreeFileContent == originalFileContent) {
                error(
                    "'${lightSyntaxTreeExpectedFile.name}' has the same content as '${originalExpectedFile.name}'. " +
                            "Remove '${lightSyntaxTreeExpectedFile.name}'"
                )
            }
        }

        checkAnnotationOwners(filePath, firFile)
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.builder.StubFirScopeProvider
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.readText


abstract class AbstractLightTree2FirConverterTestCase : AbstractRawFirBuilderTestCase() {
    @OptIn(ObsoleteTestInfrastructure::class)
    fun doTest(filePath: String) {
        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath))
        val path = Paths.get(filePath)
        val firFile = LightTree2Fir(
            session = FirSessionFactoryHelper.createEmptySession(parseLanguageFeatures(path.readText())),
            scopeProvider = StubFirScopeProvider,
            diagnosticsReporter = null
        ).buildFirFile(path)
        val firDump = FirRenderer.withDeclarationAttributes().renderElementAsString(firFile)

        val originalExpectedFile = File(expectedPath(filePath, ".txt"))
        val lightTreeExpectedFile = File(expectedPath(filePath, ".lt.txt"))
        val expectedFile = lightTreeExpectedFile.takeIf { it.exists() } ?: originalExpectedFile
        TestDataAssertions.assertEqualsToFile(expectedFile, firDump)

        if (lightTreeExpectedFile.exists()) {
            val lightTreeFileContent = lightTreeExpectedFile.readText().trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd()
            val originalFileContent = originalExpectedFile.readText().trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd()
            if (lightTreeFileContent == originalFileContent) {
                error(
                    "'${lightTreeExpectedFile.name}' has the same content as '${originalExpectedFile.name}'. " +
                            "Remove '${lightTreeExpectedFile.name}'"
                )
            }
        }

        checkAnnotationOwners(filePath, firFile)
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.FirReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.builder.StubFirScopeProvider
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirReplSnippetBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.extensions.PluginServicesInitialization
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.readText


abstract class AbstractLightTree2FirConverterTestCase : AbstractRawFirBuilderTestCase() {
    /**
     * LightTree counterpart of `KtTestUtil.createFile` marking `*.repl.kts` PSI files with `markAsReplSnippet()`:
     * the light-tree builder decides between a script and a REPL snippet through the registered
     * [FirReplSnippetConfiguratorExtension]s, so a no-op one accepting every source is registered for REPL fixtures.
     */
    private class TestReplSnippetConfigurator(session: FirSession) : FirReplSnippetConfiguratorExtension(session) {
        override fun isReplSnippetsSource(sourceFile: KtSourceFile?, scriptSource: KtSourceElement): Boolean = true
        override fun FirReplSnippetBuilder.configureContainingFile(fileBuilder: FirFileBuilder) {}
        override fun FirReplSnippetBuilder.configure(sourceFile: KtSourceFile?, context: Context<*>) {}
        override fun FirBlockBuilder.configureEvalBody(sourceFile: KtSourceFile?, scriptSource: KtSourceElement, context: Context<*>) {}
        override fun MutableList<FirElement>.configure(sourceFile: KtSourceFile?, scriptSource: KtSourceElement, context: Context<*>) {}
    }

    @OptIn(ObsoleteTestInfrastructure::class, PluginServicesInitialization::class)
    override fun runTest(filePath: String) {
        myFileExt = FileUtilRt.getExtension(PathUtil.getFileName(filePath))
        val path = Paths.get(filePath)
        val session = FirSessionFactoryHelper.createEmptySession(parseLanguageFeatures(path.readText()))
        if (filePath.endsWith(".repl.kts")) {
            session.extensionService.registerExtensions(
                FirReplSnippetConfiguratorExtension::class,
                listOf(FirReplSnippetConfiguratorExtension.Factory { TestReplSnippetConfigurator(it) }),
            )
        }
        val firFile = LightTree2Fir(
            session = session,
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

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.highlighter.markers.TestableLineMarkerNavigator
import org.jetbrains.kotlin.idea.navigation.NavigationTestUtils
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TagsTestDataUtil
import org.jetbrains.kotlin.test.util.renderAsGotoImplementation
import org.junit.Assert
import java.io.File

abstract class AbstractLineMarkersTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    fun doTest(path: String) = doTest(path) {}

    protected fun doAndCheckHighlighting(
        project: Project,
        documentToAnalyze: Document,
        expectedHighlighting: ExpectedHighlightingData,
        expectedFile: File
    ): List<LineMarkerInfo<*>> {
        myFixture.doHighlighting()

        return checkHighlighting(project, documentToAnalyze, expectedHighlighting, expectedFile)
    }

    fun doTest(path: String, additionalCheck: () -> Unit) {
        val fileText = FileUtil.loadFile(testDataFile())
        try {
            ConfigLibraryUtil.configureLibrariesByDirective(myFixture.module, PlatformTestUtil.getCommunityPath(), fileText)
            if (InTextDirectivesUtils.findStringWithPrefixes(fileText, "METHOD_SEPARATORS") != null) {
                DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS = true
            }

            myFixture.configureByFile(fileName())
            val project = myFixture.project
            val document = myFixture.editor.document

            val data = ExpectedHighlightingData(document, false, false, false, myFixture.file)
            data.init()

            PsiDocumentManager.getInstance(project).commitAllDocuments()

            val markers = doAndCheckHighlighting(myFixture.project, document, data, testDataFile())

            assertNavigationElements(myFixture.project, myFixture.file as KtFile, markers)
            additionalCheck()
        } catch (exc: Exception) {
            throw RuntimeException(exc)
        } finally {
            ConfigLibraryUtil.unconfigureLibrariesByDirective(module, fileText)
            DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS = false
        }

    }

    companion object {

        @Suppress("SpellCheckingInspection")
        private const val LINE_MARKER_PREFIX = "LINEMARKER:"
        private const val TARGETS_PREFIX = "TARGETS"

        fun assertNavigationElements(project: Project, file: KtFile, markers: List<LineMarkerInfo<*>>) {
            val navigationDataComments = KotlinTestUtils.getLastCommentsInFile(
                file, KotlinTestUtils.CommentType.BLOCK_COMMENT, false
            )
            if (navigationDataComments.isEmpty()) return

            for ((navigationCommentIndex, navigationComment) in navigationDataComments.reversed().withIndex()) {
                val description = getLineMarkerDescription(navigationComment)
                val navigateMarkers = markers.filter { it.lineMarkerTooltip?.startsWith(description) == true }
                val navigateMarker = navigateMarkers.singleOrNull() ?: navigateMarkers.getOrNull(navigationCommentIndex)

                TestCase.assertNotNull(
                    String.format("Can't find marker for navigation check with description \"%s\"", description),
                    navigateMarker
                )

                val handler = navigateMarker!!.navigationHandler
                if (handler is TestableLineMarkerNavigator) {
                    val navigateElements = handler.getTargetsPopupDescriptor(navigateMarker.element)?.targets?.sortedBy {
                        it.renderAsGotoImplementation()
                    }
                    val actualNavigationData = NavigationTestUtils.getNavigateElementsText(project, navigateElements)

                    UsefulTestCase.assertSameLines(getExpectedNavigationText(navigationComment), actualNavigationData)
                } else {
                    Assert.fail("Only TestableLineMarkerNavigator are supported in navigate check")
                }
            }
        }

        private fun getLineMarkerDescription(navigationComment: String): String {
            val firstLineEnd = navigationComment.indexOf("\n")
            TestCase.assertTrue(
                "The first line in block comment must contain description of marker for navigation check", firstLineEnd != -1
            )

            var navigationMarkerText = navigationComment.substring(0, firstLineEnd)

            TestCase.assertTrue(
                String.format("Add %s directive in first line of comment", LINE_MARKER_PREFIX),
                navigationMarkerText.startsWith(LINE_MARKER_PREFIX)
            )

            navigationMarkerText = navigationMarkerText.substring(LINE_MARKER_PREFIX.length)

            return navigationMarkerText.trim { it <= ' ' }
        }

        private fun getExpectedNavigationText(navigationComment: String): String {
            val firstLineEnd = navigationComment.indexOf("\n")

            var expectedNavigationText = navigationComment.substring(firstLineEnd + 1)

            TestCase.assertTrue(
                String.format("Marker %s is expected before navigation data", TARGETS_PREFIX),
                expectedNavigationText.startsWith(TARGETS_PREFIX)
            )

            expectedNavigationText = expectedNavigationText.substring(expectedNavigationText.indexOf("\n") + 1)

            return expectedNavigationText
        }

        fun checkHighlighting(
            project: Project,
            documentToAnalyze: Document,
            expectedHighlighting: ExpectedHighlightingData,
            expectedFile: File
        ): MutableList<LineMarkerInfo<*>> {
            val markers = DaemonCodeAnalyzerImpl.getLineMarkers(documentToAnalyze, project)

            try {
                expectedHighlighting.checkLineMarkers(markers, documentToAnalyze.text)

                // This is a workaround for sad bug in ExpectedHighlightingData:
                // the latter doesn't throw assertion error when some line markers are expected, but none are present.
                if (FileUtil.loadFile(expectedFile).contains("<lineMarker") && markers.isEmpty()) {
                    throw AssertionError("Some line markers are expected, but nothing is present at all")
                }
            } catch (error: AssertionError) {
                try {
                    val actualTextWithTestData = TagsTestDataUtil.insertInfoTags(markers, true, documentToAnalyze.text)
                    KotlinTestUtils.assertEqualsToFile(expectedFile, actualTextWithTestData)
                } catch (failure: FileComparisonFailure) {
                    throw FileComparisonFailure(
                        error.message + "\n" + failure.message,
                        failure.expected,
                        failure.actual,
                        failure.filePath
                    )
                }
            }
            return markers
        }
    }
}

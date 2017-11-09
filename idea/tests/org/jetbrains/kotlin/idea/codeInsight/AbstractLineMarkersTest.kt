/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
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
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TagsTestDataUtil
import org.jetbrains.kotlin.test.util.renderAsGotoImplementation
import org.junit.Assert
import java.io.File

abstract class AbstractLineMarkersTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getBasePath(): String {
        return PluginTestCaseBase.TEST_DATA_PROJECT_RELATIVE + "/codeInsight/lineMarker"
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
    }

    fun doTest(path: String) {
        try {
            val fileText = FileUtil.loadFile(File(path))
            ConfigLibraryUtil.configureLibrariesByDirective(myFixture.module, PlatformTestUtil.getCommunityPath(), fileText)

            myFixture.configureByFile(path)
            val project = myFixture.project
            val document = myFixture.editor.document

            val data = ExpectedHighlightingData(
                    document, false, false, false, myFixture.file)
            data.init()

            PsiDocumentManager.getInstance(project).commitAllDocuments()

            myFixture.doHighlighting()

            val markers = DaemonCodeAnalyzerImpl.getLineMarkers(document, project)

            try {
                data.checkLineMarkers(markers, document.text)

                // This is a workaround for sad bug in ExpectedHighlightingData:
                // the latter doesn't throw assertion error when some line markers are expected, but none are present.
                if (FileUtil.loadFile(File(path)).contains("<lineMarker") && markers.isEmpty()) {
                    throw AssertionError("Some line markers are expected, but nothing is present at all")
                }
            }
            catch (error: AssertionError) {
                try {
                    val actualTextWithTestData = TagsTestDataUtil.insertInfoTags(markers, true, myFixture.file.text)
                    KotlinTestUtils.assertEqualsToFile(File(path), actualTextWithTestData)
                }
                catch (failure: FileComparisonFailure) {
                    throw FileComparisonFailure(error.message + "\n" + failure.message,
                                                failure.expected,
                                                failure.actual,
                                                failure.filePath)
                }

            }

            assertNavigationElements(markers)
        }
        catch (exc: Exception) {
            throw RuntimeException(exc)
        }

    }

    private fun assertNavigationElements(markers: List<LineMarkerInfo<*>>) {
        val navigationDataComments = KotlinTestUtils.getLastCommentsInFile(
                myFixture.file as KtFile, KotlinTestUtils.CommentType.BLOCK_COMMENT, false)
        if (navigationDataComments.isEmpty()) return

        for (navigationComment in navigationDataComments) {
            val description = getLineMarkerDescription(navigationComment)
            val navigateMarker = markers.find { it.lineMarkerTooltip?.startsWith(description) == true }!!

            TestCase.assertNotNull(
                    String.format("Can't find marker for navigation check with description \"%s\"", description),
                    navigateMarker)

            val handler = navigateMarker.navigationHandler
            if (handler is TestableLineMarkerNavigator) {
                val navigateElements = handler.getTargetsPopupDescriptor(navigateMarker.element)?.targets?.sortedBy { it.renderAsGotoImplementation() }
                val actualNavigationData = NavigationTestUtils.getNavigateElementsText(myFixture.project, navigateElements)

                UsefulTestCase.assertSameLines(getExpectedNavigationText(navigationComment), actualNavigationData)
            }
            else {
                Assert.fail("Only SuperDeclarationMarkerNavigationHandler are supported in navigate check")
            }
        }
    }

    companion object {

        private val LINE_MARKER_PREFIX = "LINEMARKER:"
        private val TARGETS_PREFIX = "TARGETS"

        private fun getLineMarkerDescription(navigationComment: String): String {
            val firstLineEnd = navigationComment.indexOf("\n")
            TestCase.assertTrue("The first line in block comment must contain description of marker for navigation check", firstLineEnd != -1)

            var navigationMarkerText = navigationComment.substring(0, firstLineEnd)

            TestCase.assertTrue(String.format("Add %s directive in first line of comment", LINE_MARKER_PREFIX),
                                navigationMarkerText.startsWith(LINE_MARKER_PREFIX))

            navigationMarkerText = navigationMarkerText.substring(LINE_MARKER_PREFIX.length)

            return navigationMarkerText.trim { it <= ' ' }
        }

        private fun getExpectedNavigationText(navigationComment: String): String {
            val firstLineEnd = navigationComment.indexOf("\n")

            var expectedNavigationText = navigationComment.substring(firstLineEnd + 1)

            TestCase.assertTrue(
                    String.format("Marker %s is expected before navigation data", TARGETS_PREFIX),
                    expectedNavigationText.startsWith(TARGETS_PREFIX))

            expectedNavigationText = expectedNavigationText.substring(expectedNavigationText.indexOf("\n") + 1)

            return expectedNavigationText
        }
    }
}

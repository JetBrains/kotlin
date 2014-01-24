package org.jetbrains.jet.shortenRefs

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.jet.InTextDirectivesUtils
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.plugin.project.TargetPlatform
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.jet.lang.resolve.name.Name
import com.intellij.util.containers.Predicate
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.jet.plugin.PluginTestCaseBase
import java.io.File
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetReferenceExpression
import com.intellij.psi.PsiElement
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.JetTestCaseBuilder

abstract class AbstractShortenRefsTest : LightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = JetTestCaseBuilder.getHomeDirectory()
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(testPath: String) {
        val fixture = myFixture!!
        val dependencyPath = testPath.replace(".kt", ".dependency.kt")
        if (File(dependencyPath).exists()) {
            fixture.configureByFile(dependencyPath)
        }

        fixture.configureByFile(testPath)

        val file = fixture.getFile() as JetFile
        val selectionModel = fixture.getEditor()!!.getSelectionModel()
        if (!selectionModel.hasSelection()) error("No selection in input file")
        val element = PsiTreeUtil.findElementOfClassAtRange(file, selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), javaClass<JetElement>())!!

        CommandProcessor.getInstance()!!.executeCommand(getProject(), {
            ApplicationManager.getApplication()!!.runWriteAction {
                ShortenReferences.process(element)
            }
        }, null, null)
        selectionModel.removeSelection()

        fixture.checkResultByFile(testPath + ".after")
    }
}
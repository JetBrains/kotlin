/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.codeInsight.daemon.ProblemHighlightFilter
import com.intellij.codeInsight.intention.IntentionManager
import com.intellij.ide.impl.ProjectUtil
import com.intellij.idea.IdeaTestApplication
import com.intellij.lang.ExternalAnnotatorsFilter
import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.StdLanguages
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.search.IndexPatternBuilder
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.xml.XmlFileNSInfoProvider
import com.intellij.xml.XmlSchemaProvider
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.perf.Stats.Companion.tcSuite
import org.jetbrains.kotlin.idea.perf.Stats.Companion.tcTest
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import java.io.*

abstract class WholeProjectPerformanceTest : DaemonAnalyzerTestCase(), WholeProjectFileProvider {

    private val rootProjectFile: File = File("../perfTestProject").absoluteFile
    private val perfStats: Stats = Stats(name = "whole", header = arrayOf("File", "ProcessID", "Time"))
    private val tmp = rootProjectFile

    override fun isStressTest(): Boolean = false

    override fun setUp() {

        IdeaTestApplication.getInstance()
        // to prevent leaked SDKs: 1.8 and Kotlin SDK
        runWriteAction {
            val jdkTableImpl = JavaAwareProjectJdkTableImpl.getInstanceEx()
            val homePath = if (jdkTableImpl.internalJdk.homeDirectory!!.name == "jre") {
                jdkTableImpl.internalJdk.homeDirectory!!.parent.path
            } else {
                jdkTableImpl.internalJdk.homePath!!
            }

            val javaSdk = JavaSdk.getInstance()
            val j8 = javaSdk.createJdk("1.8", homePath)

            val jdkTable = getProjectJdkTableSafe()
            jdkTable.addJdk(j8, testRootDisposable)
            KotlinSdkType.setUpIfNeeded()
        }

        super.setUp()

        // fix due to stress test disabled
        IntentionManager.getInstance().availableIntentionActions  // hack to avoid slowdowns in PyExtensionFactory
        // don't need test data
        //PathManagerEx.getTestDataPath() // to cache stuff
        ReferenceProvidersRegistry.getInstance() // pre-load tons of classes
        InjectedLanguageManager.getInstance(project) // zillion of Dom Sem classes
        LanguageAnnotators.INSTANCE.allForLanguage(JavaLanguage.INSTANCE) // pile of annotator classes loads
        LanguageAnnotators.INSTANCE.allForLanguage(StdLanguages.XML)
        assertTrue(
            "side effect: to load extensions",
            ProblemHighlightFilter.EP_NAME.extensions.toMutableList()
                .plus(ImplicitUsageProvider.EP_NAME.extensions)
                .plus(XmlSchemaProvider.EP_NAME.extensions)
                .plus(XmlFileNSInfoProvider.EP_NAME.extensions)
                .plus(ExternalAnnotatorsFilter.EXTENSION_POINT_NAME.extensions)
                .plus(IndexPatternBuilder.EP_NAME.extensions).isNotEmpty()
        )
    }

    override fun setUpProject() {
        println("Using project in $tmp")

        (ApplicationManager.getApplication() as ApplicationEx).isSaveAllowed = false
        myProject = ProjectUtil.openOrImport(tmp.path, null, false)
    }

    data class PerFileTestResult(val perProcess: Map<String, Long>, val totalNs: Long, val errors: List<Throwable>)

    protected abstract fun doTest(file: VirtualFile): PerFileTestResult

    fun testWholeProjectPerformance() {

        tcSuite(this::class.simpleName ?: "Unknown") {
            val totals = mutableMapOf<String, Long>()

            fun appendInspectionResult(file: String, id: String, nanoTime: Long) {
                totals.merge(id, nanoTime) { a, b -> a + b }

                perfStats.append(file, id, nanoTime)
            }

            tcSuite("TotalPerFile") {
                // TODO: [VD] temp to limit number of files (and total time to run)
                val files = provideFiles(project).sortedBy { it.name }.take(10)

                files.forEach {
                    val filePath = File(it.path).relativeTo(tmp).path.replace(File.separatorChar, '/')
                    tcTest(filePath) {
                        val result = doTest(it)
                        result.perProcess.forEach { (k, v) ->
                            appendInspectionResult(filePath, k, v)
                        }
                        (result.totalNs.nsToMs) to result.errors
                    }
                }
            }

            tcSuite("Total") {
                totals.forEach { (k, v) ->
                    tcTest(k) {
                        v.nsToMs to emptyList()
                    }
                }
            }
        }
    }

    protected fun PsiElement.acceptRecursively(visitor: PsiElementVisitor) {
        this.accept(visitor)
        for (child in this.children) {
            child.acceptRecursively(visitor)
        }
    }

    companion object {
        val Long.nsToMs get() = (this * 1e-6).toLong()
    }

}
/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.impl.FileTypeManagerImpl
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.ModuleTestCase
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.doFirResolveTestBench
import org.jetbrains.kotlin.fir.java.FirJavaModuleBasedSession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.progress
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.fir.IdeFirDependenciesSymbolProvider
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class FirTotalKotlinResolveInIdeTest : ModuleTestCase() {
    private val projectRootFile = File(".")

    override fun setUpProject() {
        (ApplicationManager.getApplication() as ApplicationEx).doNotSave()
        myProject = ProjectUtil.openOrImport(projectRootFile.path, null, false)
        LightPlatformTestCase.clearUncommittedDocuments(this.project)
        this.runStartupActivities()
        (FileTypeManager.getInstance() as FileTypeManagerImpl).drainReDetectQueue()
    }

    private lateinit var sessionProvider: FirProjectSessionProvider

    private fun IdeaModuleInfo.createSession(): FirSession {
        val moduleInfo = this

        return FirJavaModuleBasedSession(
            moduleInfo, sessionProvider, moduleInfo.contentScope(),
            IdeFirDependenciesSymbolProvider(moduleInfo as ModuleSourceInfo, project, sessionProvider)
        )
    }

    override fun setUp() {
        super.setUp()
        sessionProvider = FirProjectSessionProvider(project)
    }

    override fun tearDown() {
        val jdkTable = ProjectJdkTable.getInstance()
        runWriteAction {
            for (sdk in jdkTable.allJdks) {
                jdkTable.removeJdk(sdk)
            }
        }
        super.tearDown()
    }

    fun testTotalKotlin() {
        val psiManager = PsiManager.getInstance(project)
        val files = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, GlobalSearchScope.projectScope(project))
        val firFiles = mutableListOf<FirFile>()
        val sessionPerModule = mutableMapOf<ModuleInfo, FirSession>()

        println("Got vfiles: ${files.size}")
        files.mapNotNull {
            val file = psiManager.findFile(it) as? KtFile ?: return@mapNotNull null
            val moduleInfo = file.getNullableModuleInfo() as? ModuleSourceInfo ?: return@mapNotNull null
            file to moduleInfo
        }.progress("Loading FIR").forEach { (file, moduleInfo) ->

            val session = sessionPerModule.getOrPut(moduleInfo) {
                moduleInfo.createSession()
            }
            val builder = RawFirBuilder(session, stubMode = true)

            try {
                val firFile = builder.buildFirFile(file)
                (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile)
                firFiles += firFile
            } catch (e: Exception) {
                System.err.println("Error building fir for $file")
                e.printStackTrace()
            }
        }

        println("Raw fir up, files: ${firFiles.size}")
        doFirResolveTestBench(firFiles, FirTotalResolveTransformer().transformers, withProgress = true)
    }
}
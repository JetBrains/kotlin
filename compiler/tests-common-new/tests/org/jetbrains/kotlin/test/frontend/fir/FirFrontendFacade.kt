/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.fir.analysis.FirAnalyzerFacade
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.session.FirJvmModuleInfo
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

class FirFrontendFacade(
    testServices: TestServices
) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirModuleInfoProvider))

    override val additionalDirectives: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun analyze(module: TestModule): FirOutputArtifact {
        val moduleInfoProvider = testServices.firModuleInfoProvider
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        // TODO: add configurable parser

        val project = compilerConfigurationProvider.getProject(module)

        PsiElementFinder.EP.getPoint(project).unregisterExtension(JavaElementFinder::class.java)

        val lightTreeEnabled = FirDiagnosticsDirectives.USE_LIGHT_TREE in module.directives
        val (ktFiles, originalFiles) = if (lightTreeEnabled) {
            emptyList<KtFile>() to module.files.filter { it.isKtFile }.map { testServices.sourceFileProvider.getRealFileForSourceFile(it) }
        } else {
            testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project).values to emptyList()
        }

        val sessionProvider = moduleInfoProvider.firSessionProvider

        val languageVersionSettings = module.languageVersionSettings
        val builtinsModuleInfo = moduleInfoProvider.builtinsModuleInfoForModule(module)
        val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)

        createSessionForBuiltins(builtinsModuleInfo, sessionProvider, project, packagePartProviderFactory)
        createSessionForBinaryDependencies(module, sessionProvider, project, packagePartProviderFactory)

        val sourcesScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)
        val sourcesModuleInfo = moduleInfoProvider.convertToFirModuleInfo(module)
        val session = FirSessionFactory.createJavaModuleBasedSession(
            sourcesModuleInfo,
            sessionProvider,
            sourcesScope,
            project,
            languageVersionSettings = languageVersionSettings
        ) {
            if (FirDiagnosticsDirectives.WITH_EXTENDED_CHECKERS in module.directives) {
                registerExtendedCommonCheckers()
            }
        }

        val firAnalyzerFacade = FirAnalyzerFacade(session, languageVersionSettings, ktFiles, originalFiles, lightTreeEnabled)
        val firFiles = firAnalyzerFacade.runResolution()
        val filesMap = firFiles.mapNotNull { firFile ->
            val testFile = module.files.firstOrNull { it.name == firFile.name } ?: return@mapNotNull null
            testFile to firFile
        }.toMap()

        return FirOutputArtifactImpl(session, filesMap, firAnalyzerFacade)
    }

    private fun createSessionForBuiltins(
        builtinsModuleInfo: FirJvmModuleInfo,
        sessionProvider: FirProjectSessionProvider,
        project: Project,
        packagePartProviderFactory: (GlobalSearchScope) -> JvmPackagePartProvider,
    ) {
        //For BuiltIns, registered in sessionProvider automatically
        val allProjectScope = GlobalSearchScope.allScope(project)

        FirSessionFactory.createLibrarySession(
            builtinsModuleInfo, sessionProvider, allProjectScope, project,
            packagePartProviderFactory(allProjectScope)
        )
    }

    private fun createSessionForBinaryDependencies(
        module: TestModule,
        sessionProvider: FirProjectSessionProvider,
        project: Project,
        packagePartProviderFactory: (GlobalSearchScope) -> JvmPackagePartProvider,
    ) {
        val librariesScope = ProjectScope.getLibrariesScope(project)
        val librariesModuleInfo = FirJvmModuleInfo.createForLibraries(module.name)
        FirSessionFactory.createLibrarySession(
            librariesModuleInfo,
            sessionProvider,
            librariesScope,
            project,
            packagePartProviderFactory(librariesScope)
        )
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtension
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionFile
import org.jetbrains.kotlin.analysis.api.resolve.extensions.KtResolveExtensionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.directives.publishModificationEventByDirective
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * A simple test which detects when resolve extension disposal after modification events/session invalidation doesn't work *at all*.
 */
abstract class AbstractResolveExtensionDisposalAfterModificationEventTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ) {
        val session = LLFirSessionCache.getInstance(mainFile.project).getSession(mainModule.ktModule)
        val resolveExtension = session.llResolveExtensionTool!!.extensions.single() as KtResolveExtensionWithDisposalTracker

        testServices.assertions.assertFalse(resolveExtension.isDisposed) {
            "The resolve extension should not be disposed before the modification event is published."
        }

        mainModule.publishModificationEventByDirective()

        testServices.assertions.assertTrue(resolveExtension.isDisposed) {
            "The resolve extension should be disposed after the modification event has been published."
        }
    }

    override val configurator: AnalysisApiTestConfigurator
        get() = ResolveExtensionDisposalTestConfigurator
}

class KtResolveExtensionWithDisposalTracker() : KtResolveExtension() {
    override fun getKtFiles(): List<KtResolveExtensionFile> = emptyList()
    override fun getContainedPackages(): Set<FqName> = emptySet()
    override fun getShadowedScope(): GlobalSearchScope = GlobalSearchScope.EMPTY_SCOPE

    var isDisposed: Boolean = false

    override fun dispose() {
        isDisposed = true
    }
}

class KtResolveExtensionWithDisposalTrackerProvider() : KtResolveExtensionProvider() {
    override fun provideExtensionsFor(module: KtModule): List<KtResolveExtension> = listOf(KtResolveExtensionWithDisposalTracker())
}

object ResolveExtensionDisposalTestConfigurator : AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false) {
    override val serviceRegistrars: List<AnalysisApiTestServiceRegistrar>
        get() = buildList {
            addAll(super.serviceRegistrars)
            add(ResolveExtensionDisposalTestServiceRegistrar)
        }
}

object ResolveExtensionDisposalTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {}
    override fun registerProjectExtensionPoints(project: MockProject, testServices: TestServices) {}
    override fun registerProjectModelServices(project: MockProject, testServices: TestServices) {}

    override fun registerProjectServices(
        project: MockProject,
        testServices: TestServices,
    ) {
        val extensionPoint = project.extensionArea.getExtensionPoint(KtResolveExtensionProvider.EP_NAME)
        extensionPoint.registerExtension(KtResolveExtensionWithDisposalTrackerProvider(), project)
    }
}

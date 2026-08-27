package org.jetbrains.kotlin.analysis.api.fir.test

import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.platform.KaSessionListener
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class KaSessionListenerTest : AbstractAnalysisApiExecutionTest("testData/sessionListener") {
    override val configurator = LLSourceLikeTestConfigurator()

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = super.additionalServiceRegistrars + listOf(KaSessionListenerTestServiceRegistrar)

    data class TestResult(val trace: List<String>, val thrown: Throwable?)

    private fun runSessionHookTest(
        mainFile: KtFile,
        testServices: TestServices,
        analyzeBlock: context(KaSession) () -> Unit = {},
    ): TestResult {
        val events = testServices.kaSessionListenerTestEvents.events
        var thrown: Throwable? = null
        val simpleClass = mainFile.declarations.single { it is KtClass && it.name == "Simple" } as KtClass
        events.add("before 'analyze' block")
        try {
            analyze(simpleClass) {
                events.add("inside 'analyze' block")
                analyzeBlock()
            }
        } catch (e: Throwable) {
            thrown = e
            events.add("caught exception")
        }
        events.add("after 'analyze' block")
        return TestResult(events, thrown)
    }

    @Test
    fun testSessionListener(mainFile: KtFile, testServices: TestServices) {
        val result = runSessionHookTest(mainFile, testServices)
        assertNull(result.thrown)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L1.afterAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "L2.beforeEnteringAnalysis",
                "inside 'analyze' block",
                "L1.afterLeavingAnalysis",
                "L2.afterLeavingAnalysis",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testAnalysisException(mainFile: KtFile, testServices: TestServices) {
        val result = runSessionHookTest(mainFile, testServices) {
            throw RuntimeException("fail")
        }
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L1.afterAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "L2.beforeEnteringAnalysis",
                "inside 'analyze' block",
                "L1.onAnalysisException",
                "L2.onAnalysisException",
                "L1.afterLeavingAnalysis",
                "L2.afterLeavingAnalysis",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }
}

private class KaSessionListenerTestEventsService : TestService {
    val events: MutableList<String> = mutableListOf()
}

private val TestServices.kaSessionListenerTestEvents: KaSessionListenerTestEventsService by TestServices.testServiceAccessor()

private object KaSessionListenerTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerApplicationServices(
        application: MockApplication,
        disposable: Disposable,
        testServices: TestServices,
    ) {
        testServices.register(KaSessionListenerTestEventsService::class, KaSessionListenerTestEventsService())

        val extensionPoint = application.extensionArea.getExtensionPoint(KaSessionListener.EP_NAME)
        extensionPoint.registerExtension(TestKaSessionListener("L1", testServices), disposable)
        extensionPoint.registerExtension(TestKaSessionListener("L2", testServices), disposable)
    }

    private class TestKaSessionListener(
        private val name: String,
        private val testServices: TestServices,
    ) : KaSessionListener {
        private fun record(hookName: String) {
            testServices.kaSessionListenerTestEvents.events.add("$name.$hookName")
        }

        override fun beforeAcquiringSession(useSiteModule: KaModule, useSiteElement: PsiElement?) =
            record("beforeAcquiringSession")

        override fun onSessionAcquisitionException(useSiteModule: KaModule, useSiteElement: PsiElement?, throwable: Throwable) =
            record("onSessionAcquisitionException")

        override fun afterAcquiringSession(useSiteModule: KaModule, useSiteElement: PsiElement?) =
            record("afterAcquiringSession")

        override fun beforeEnteringAnalysis(useSiteModule: KaModule, useSiteElement: PsiElement?) =
            record("beforeEnteringAnalysis")

        override fun onAnalysisException(useSiteModule: KaModule, useSiteElement: PsiElement?, throwable: Throwable) =
            record("onAnalysisException")

        override fun afterLeavingAnalysis(useSiteModule: KaModule, useSiteElement: PsiElement?) =
            record("afterLeavingAnalysis")
    }
}

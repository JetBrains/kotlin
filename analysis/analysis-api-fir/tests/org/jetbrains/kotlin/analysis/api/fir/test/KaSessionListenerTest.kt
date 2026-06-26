package org.jetbrains.kotlin.analysis.api.fir.test

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.platform.KaSessionListener
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class KaSessionListenerTest : AbstractAnalysisApiExecutionTest("testData/sessionListener") {
    override val configurator = LLSourceLikeTestConfigurator()

    open class TestKaSessionListener(
        val name: String,
        val events: MutableList<String>,
    ) : KaSessionListener {
        private fun record(hookName: String) {
            events.add("$name.$hookName")
        }

        override fun beforeAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) =
            record("beforeAcquiringSession")

        override fun onSessionAcquisitionException(useSiteModule: KaModule, useSiteElement: KtElement?, throwable: Throwable) =
            record("onSessionAcquisitionException")

        override fun afterAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) =
            record("afterAcquiringSession")

        override fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement?) =
            record("beforeEnteringAnalysis")

        override fun onAnalysisException(session: KaSession, useSiteElement: KtElement?, throwable: Throwable) =
            record("onAnalysisException")

        override fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement?) =
            record("afterLeavingAnalysis")
    }

    data class TestResult(val trace: List<String>, val thrown: Throwable?)

    private fun runSessionHookTest(
        mainFile: KtFile,
        events: MutableList<String>,
        listeners: List<TestKaSessionListener>,
        analyzeBlock: KaSession.() -> Unit = {},
    ): TestResult {
        val project = mainFile.project
        val disposable = Disposer.newDisposable("KaSessionListenerTest")
        var thrown: Throwable? = null
        try {
            listeners.forEach { listener ->
                project.extensionArea.getExtensionPoint(KaSessionListener.EP_NAME).registerExtension(listener, disposable)
            }
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
        } finally {
            Disposer.dispose(disposable)
        }
        return TestResult(events, thrown)
    }

    @Test
    fun testSessionListener(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val listener = TestKaSessionListener("L1", events)
        val result = runSessionHookTest(mainFile, events, listOf(listener))
        assertNull(result.thrown)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L1.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "inside 'analyze' block",
                "L1.afterLeavingAnalysis",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testAnalysisException(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val listener = TestKaSessionListener("L1", events)
        val result = runSessionHookTest(mainFile, events, listOf(listener)) {
            throw RuntimeException("fail")
        }
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L1.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "inside 'analyze' block",
                "L1.onAnalysisException",
                "L1.afterLeavingAnalysis",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testThreeListenersNoExceptions(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = TestKaSessionListener("L1", events)
        val l2 = TestKaSessionListener("L2", events)
        val l3 = TestKaSessionListener("L3", events)

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2, l3))
        assertNull(result.thrown)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L3.beforeAcquiringSession",
                "L1.afterAcquiringSession",
                "L2.afterAcquiringSession",
                "L3.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "L2.beforeEnteringAnalysis",
                "L3.beforeEnteringAnalysis",
                "inside 'analyze' block",
                "L1.afterLeavingAnalysis",
                "L2.afterLeavingAnalysis",
                "L3.afterLeavingAnalysis",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    class TestLoggerFactory : Logger.Factory {
        override fun getLoggerInstance(category: String): Logger = testLogger

        companion object {
            val loggedErrors = mutableListOf<Throwable>()

            val testLogger: Logger = object : Logger() {
                override fun isDebugEnabled(): Boolean = false
                override fun debug(message: String?) {}
                override fun debug(t: Throwable?) {}
                override fun debug(message: String?, t: Throwable?) {}
                override fun info(message: String?) {}
                override fun info(message: String?, t: Throwable?) {}
                override fun warn(message: String?, t: Throwable?) {}
                override fun error(message: String?, t: Throwable?, vararg details: String?) {
                    if (t != null) {
                        loggedErrors.add(t)
                    }
                }
            }
        }
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testListenerExceptionLoggedToLogger(mainFile: KtFile) {
        Logger.setFactory(TestLoggerFactory::class.java)
        TestLoggerFactory.loggedErrors.clear()

        val exceptionToThrow = RuntimeException("Listener error")
        val throwingListener = object : KaSessionListener {
            override fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement?) {
                throw exceptionToThrow
            }
        }

        val events = mutableListOf<String>()
        val normalListener = TestKaSessionListener("L1", events)

        val disposable = Disposer.newDisposable("testListenerExceptionLoggedToLogger")
        try {
            val ep = mainFile.project.extensionArea.getExtensionPoint(KaSessionListener.EP_NAME)
            ep.registerExtension(throwingListener, disposable)
            ep.registerExtension(normalListener, disposable)

            val simpleClass = mainFile.declarations.single { it is KtClass && it.name == "Simple" } as KtClass
            analyze(simpleClass) {}

            assertEquals(1, TestLoggerFactory.loggedErrors.size)
            assertSame(exceptionToThrow, TestLoggerFactory.loggedErrors.single())
            assertEquals(
                listOf(
                    "L1.beforeAcquiringSession",
                    "L1.afterAcquiringSession",
                    "L1.beforeEnteringAnalysis",
                    "L1.afterLeavingAnalysis"
                ),
                events
            )
        } finally {
            Disposer.dispose(disposable)
        }
    }
}

package org.jetbrains.kotlin.analysis.api.fir.test

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.session.KaSessionListener
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

class KaSessionListenerTest : AbstractAnalysisApiExecutionTest("testData/sessionListener") {
    override val configurator = LLSourceLikeTestConfigurator()

    @OptIn(KaExperimentalApi::class)
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

    @OptIn(KaExperimentalApi::class)
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
                "L3.afterAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "L2.beforeEnteringAnalysis",
                "L3.beforeEnteringAnalysis",
                "inside 'analyze' block",
                "L3.afterLeavingAnalysis",
                "L2.afterLeavingAnalysis",
                "L1.afterLeavingAnalysis",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testTwoListenersExceptionFromFirst(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = object : TestKaSessionListener("L1", events) {
            override fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement?) {
                super.beforeEnteringAnalysis(session, useSiteElement)
                throw RuntimeException("L1 beforeEnteringAnalysis fail")
            }
        }
        val l2 = TestKaSessionListener("L2", events)

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L1 beforeEnteringAnalysis fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testThreeListenersExceptionFromSecond(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = TestKaSessionListener("L1", events)
        val l2 = object : TestKaSessionListener("L2", events) {
            override fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement?) {
                super.beforeEnteringAnalysis(session, useSiteElement)
                throw RuntimeException("L2 beforeEnteringAnalysis fail")
            }
        }
        val l3 = TestKaSessionListener("L3", events)

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2, l3))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L2 beforeEnteringAnalysis fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L3.beforeAcquiringSession",
                "L3.afterAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "L2.beforeEnteringAnalysis",
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
    fun testTwoListenersExceptionFromFirstAfter(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = object : TestKaSessionListener("L1", events) {
            override fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement?) {
                super.afterLeavingAnalysis(session, useSiteElement)
                throw RuntimeException("L1 afterLeavingAnalysis fail")
            }
        }
        val l2 = TestKaSessionListener("L2", events)

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L1 afterLeavingAnalysis fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "L2.beforeEnteringAnalysis",
                "inside 'analyze' block",
                "L2.afterLeavingAnalysis",
                "L1.afterLeavingAnalysis",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testTwoListenersExceptionFromSecondAfter(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = TestKaSessionListener("L1", events)
        val l2 = object : TestKaSessionListener("L2", events) {
            override fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement?) {
                super.afterLeavingAnalysis(session, useSiteElement)
                throw RuntimeException("L2 afterLeavingAnalysis fail")
            }
        }

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L2 afterLeavingAnalysis fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "L2.beforeEnteringAnalysis",
                "inside 'analyze' block",
                "L2.afterLeavingAnalysis",
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
    fun testThreeListenersExceptionFromSecondAcquiring(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = TestKaSessionListener("L1", events)
        val l2 = object : TestKaSessionListener("L2", events) {
            override fun beforeAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {
                super.beforeAcquiringSession(useSiteModule, useSiteElement)
                throw RuntimeException("L2 beforeAcquiringSession fail")
            }
        }
        val l3 = TestKaSessionListener("L3", events)

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2, l3))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L2 beforeAcquiringSession fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L1.onSessionAcquisitionException",
                "L1.afterAcquiringSession",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testTwoListenersExceptionFromSecondAfterAcquiring(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = TestKaSessionListener("L1", events)
        val l2 = object : TestKaSessionListener("L2", events) {
            override fun afterAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {
                super.afterAcquiringSession(useSiteModule, useSiteElement)
                throw RuntimeException("L2 afterAcquiringSession fail")
            }
        }

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L2 afterAcquiringSession fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.onSessionAcquisitionException",
                "L1.afterAcquiringSession",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testAnalysisExceptionThrowingInOnAnalysisException(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = object : TestKaSessionListener("L1", events) {
            override fun onAnalysisException(session: KaSession, useSiteElement: KtElement?, throwable: Throwable) {
                events.add("L1.onAnalysisException:${throwable.message}")
            }
        }
        val l2 = object : TestKaSessionListener("L2", events) {
            override fun onAnalysisException(session: KaSession, useSiteElement: KtElement?, throwable: Throwable) {
                super.onAnalysisException(session, useSiteElement, throwable)
                throw RuntimeException("L2 onAnalysisException fail")
            }
        }

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2)) {
            throw RuntimeException("fail")
        }
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L2 onAnalysisException fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "L2.beforeEnteringAnalysis",
                "inside 'analyze' block",
                "L2.onAnalysisException",
                "L1.onAnalysisException:L2 onAnalysisException fail",
                "L2.afterLeavingAnalysis",
                "L1.afterLeavingAnalysis",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testTwoListenersExceptionFromSecondAfterAndOtherHooksThrow(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = object : TestKaSessionListener("L1", events) {
            override fun onAnalysisException(session: KaSession, useSiteElement: KtElement?, throwable: Throwable) {
                events.add("L1.onAnalysisException:${throwable.message}")
                throw RuntimeException("L1 onAnalysisException fail")
            }

            override fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement?) {
                super.afterLeavingAnalysis(session, useSiteElement)
                throw RuntimeException("L1 afterLeavingAnalysis fail")
            }
        }
        val l2 = object : TestKaSessionListener("L2", events) {
            override fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement?) {
                super.afterLeavingAnalysis(session, useSiteElement)
                throw RuntimeException("L2 afterLeavingAnalysis fail")
            }
        }

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L1 afterLeavingAnalysis fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "L2.beforeEnteringAnalysis",
                "inside 'analyze' block",
                "L2.afterLeavingAnalysis",
                "L1.onAnalysisException:L2 afterLeavingAnalysis fail",
                "L1.afterLeavingAnalysis",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testTwoListenersExceptionFromSecondAcquiringAndOtherHooksThrow(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = object : TestKaSessionListener("L1", events) {
            override fun onSessionAcquisitionException(
                useSiteModule: KaModule,
                useSiteElement: KtElement?,
                throwable: Throwable,
            ) {
                events.add("L1.onSessionAcquisitionException:${throwable.message}")
                throw RuntimeException("L1 onSessionAcquisitionException fail")
            }

            override fun afterAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {
                super.afterAcquiringSession(useSiteModule, useSiteElement)
                throw RuntimeException("L1 afterAcquiringSession fail")
            }
        }
        val l2 = object : TestKaSessionListener("L2", events) {
            override fun beforeAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {
                super.beforeAcquiringSession(useSiteModule, useSiteElement)
                throw RuntimeException("L2 beforeAcquiringSession fail")
            }
        }

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L1 afterAcquiringSession fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L1.onSessionAcquisitionException:L2 beforeAcquiringSession fail",
                "L1.afterAcquiringSession",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testTwoListenersExceptionFromFirstAcquiring(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = object : TestKaSessionListener("L1", events) {
            override fun beforeAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {
                super.beforeAcquiringSession(useSiteModule, useSiteElement)
                throw RuntimeException("L1 beforeAcquiringSession fail")
            }
        }
        val l2 = TestKaSessionListener("L2", events)

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L1 beforeAcquiringSession fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testTwoListenersExceptionFromFirstAfterAcquiring(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = object : TestKaSessionListener("L1", events) {
            override fun afterAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {
                super.afterAcquiringSession(useSiteModule, useSiteElement)
                throw RuntimeException("L1 afterAcquiringSession fail")
            }
        }
        val l2 = TestKaSessionListener("L2", events)

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L1 afterAcquiringSession fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.afterAcquiringSession",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testSessionAcquisitionExceptionThrowingInOnSessionAcquisitionException(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = object : TestKaSessionListener("L1", events) {
            override fun onSessionAcquisitionException(
                useSiteModule: KaModule,
                useSiteElement: KtElement?,
                throwable: Throwable,
            ) {
                events.add("L1.onSessionAcquisitionException:${throwable.message}")
            }
        }
        val l2 = object : TestKaSessionListener("L2", events) {
            override fun onSessionAcquisitionException(
                useSiteModule: KaModule,
                useSiteElement: KtElement?,
                throwable: Throwable,
            ) {
                super.onSessionAcquisitionException(useSiteModule, useSiteElement, throwable)
                throw RuntimeException("L2 onSessionAcquisitionException fail")
            }
        }
        val l3 = object : TestKaSessionListener("L3", events) {
            override fun beforeAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {
                super.beforeAcquiringSession(useSiteModule, useSiteElement)
                throw RuntimeException("L3 beforeAcquiringSession fail")
            }
        }

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2, l3))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L2 onSessionAcquisitionException fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L3.beforeAcquiringSession",
                "L2.onSessionAcquisitionException",
                "L1.onSessionAcquisitionException:L2 onSessionAcquisitionException fail",
                "L2.afterAcquiringSession",
                "L1.afterAcquiringSession",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testTwoListenersExceptionFromSecondAfterAcquiringAndOtherHooksThrow(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = object : TestKaSessionListener("L1", events) {
            override fun onSessionAcquisitionException(
                useSiteModule: KaModule,
                useSiteElement: KtElement?,
                throwable: Throwable,
            ) {
                events.add("L1.onSessionAcquisitionException:${throwable.message}")
                throw RuntimeException("L1 onSessionAcquisitionException fail")
            }

            override fun afterAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {
                super.afterAcquiringSession(useSiteModule, useSiteElement)
                throw RuntimeException("L1 afterAcquiringSession fail")
            }
        }
        val l2 = object : TestKaSessionListener("L2", events) {
            override fun afterAcquiringSession(useSiteModule: KaModule, useSiteElement: KtElement?) {
                super.afterAcquiringSession(useSiteModule, useSiteElement)
                throw RuntimeException("L2 afterAcquiringSession fail")
            }
        }

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L1 afterAcquiringSession fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.onSessionAcquisitionException:L2 afterAcquiringSession fail",
                "L1.afterAcquiringSession",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }

    @Test
    @TestMetadata("testSessionListener")
    fun testTwoListenersExceptionFromSecondBeforeAndOtherHooksThrow(mainFile: KtFile) {
        val events = mutableListOf<String>()
        val l1 = object : TestKaSessionListener("L1", events) {
            override fun onAnalysisException(session: KaSession, useSiteElement: KtElement?, throwable: Throwable) {
                events.add("L1.onAnalysisException:${throwable.message}")
                throw RuntimeException("L1 onAnalysisException fail")
            }

            override fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement?) {
                super.afterLeavingAnalysis(session, useSiteElement)
                throw RuntimeException("L1 afterLeavingAnalysis fail")
            }
        }
        val l2 = object : TestKaSessionListener("L2", events) {
            override fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement?) {
                super.beforeEnteringAnalysis(session, useSiteElement)
                throw RuntimeException("L2 beforeEnteringAnalysis fail")
            }
        }

        val result = runSessionHookTest(mainFile, events, listOf(l1, l2))
        val thrown = result.thrown
        assertNotNull(thrown)
        assertEquals("L1 afterLeavingAnalysis fail", thrown.message)
        assertEquals(
            listOf(
                "before 'analyze' block",
                "L1.beforeAcquiringSession",
                "L2.beforeAcquiringSession",
                "L2.afterAcquiringSession",
                "L1.afterAcquiringSession",
                "L1.beforeEnteringAnalysis",
                "L2.beforeEnteringAnalysis",
                "L1.onAnalysisException:L2 beforeEnteringAnalysis fail",
                "L1.afterLeavingAnalysis",
                "caught exception",
                "after 'analyze' block"
            ),
            result.trace
        )
    }
}

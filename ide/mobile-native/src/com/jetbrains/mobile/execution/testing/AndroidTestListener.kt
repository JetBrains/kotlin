package com.jetbrains.mobile.execution.testing

import com.android.ddmlib.testrunner.ITestRunListener
import com.android.ddmlib.testrunner.TestIdentifier
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.JavaTestLocator
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.URLUtil
import com.jetbrains.mobile.execution.AndroidProcessHandler

class AndroidTestListener(private val handler: AndroidProcessHandler) : ITestRunListener {
    private var testSuite: Pair<String, /* startedTime: */ Long>? = null
    private var testStartedTime: Long? = null

    private fun ServiceMessageBuilder.send() {
        handler.notifyTextAvailable(toString() + "\n", ProcessOutputType.SYSTEM)
    }

    private fun suiteFinished() {
        testSuite?.let {
            val fqClassName = it.first
            ServiceMessageBuilder.testSuiteFinished(StringUtil.getShortName(fqClassName))
                .addAttribute("duration", (System.currentTimeMillis() - it.second).toString())
                .send()
        }
    }

    override fun testRunStarted(runName: String, testCount: Int) {
        ServiceMessageBuilder("enteredTheMatrix").send()
    }

    override fun testStarted(test: TestIdentifier) {
        val fqClassName = test.className
        if (testSuite?.first != fqClassName) {
            suiteFinished()
            ServiceMessageBuilder.testSuiteStarted(StringUtil.getShortName(fqClassName))
                .addAttribute("locationHint", JavaTestLocator.SUITE_PROTOCOL + URLUtil.SCHEME_SEPARATOR + fqClassName)
                .send()
            testSuite = fqClassName to System.currentTimeMillis()
        }
        testStartedTime = System.currentTimeMillis()
        val fqTestName = StringUtil.getQualifiedName(fqClassName, test.testName)
        ServiceMessageBuilder.testStarted(test.testName)
            .addAttribute("locationHint", JavaTestLocator.TEST_PROTOCOL + URLUtil.SCHEME_SEPARATOR + fqTestName)
            .send()
    }

    override fun testFailed(test: TestIdentifier, trace: String) {
        ServiceMessageBuilder.testFailed(test.testName)
            .addAttribute("message", trace)
            .send()
    }

    override fun testEnded(test: TestIdentifier, testMetrics: Map<String, String>) {
        ServiceMessageBuilder.testFinished(test.testName)
            .addAttribute("duration", (System.currentTimeMillis() - testStartedTime!!).toString())
            .send()
    }

    override fun testIgnored(test: TestIdentifier) {
        ServiceMessageBuilder.testIgnored(test.testName).send()
    }

    override fun testAssumptionFailure(test: TestIdentifier, trace: String) {
        ServiceMessageBuilder.testIgnored(test.testName)
            .addAttribute("message", trace)
            .send()
    }

    override fun testRunFailed(errorMessage: String) {
        handler.notifyTextAvailable("Execution failed: $errorMessage", ProcessOutputType.SYSTEM)
        handler.destroyProcess()
    }

    override fun testRunStopped(elapsedTime: Long) {
        handler.destroyProcess()
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {
        suiteFinished()
        handler.destroyProcess()
    }
}
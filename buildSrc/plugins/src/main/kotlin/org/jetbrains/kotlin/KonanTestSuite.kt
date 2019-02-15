package org.jetbrains.kotlin

import java.io.PrintWriter
import java.io.StringWriter

enum class TestStatus {
    PASSED,
    FAILED,
    ERROR,
    SKIPPED
}

data class TestResult(val status: TestStatus, val comment: String = "")

data class Statistics(
        var passed: Int = 0,
        var failed: Int = 0,
        var error: Int = 0,
        var skipped: Int = 0) {

    val total: Int
        get() = passed + failed + error + skipped

    fun pass(count: Int = 1) { passed += count }

    fun skip(count: Int = 1) { skipped += count }

    fun fail(count: Int = 1) { failed += count }

    fun error(count: Int = 1) { error += count }

    fun add(other: Statistics) {
        passed += other.passed
        failed += other.failed
        error += other.error
        skipped += other.skipped
    }
}

open class KonanTestSuite(val name: String, val statistics: Statistics) {

    open inner class KonanTestCase(val name: String) {
        open fun start() {}

        open fun pass(): TestResult {
            statistics.pass()
            return TestResult(TestStatus.PASSED)
        }

        open fun fail(e: RuntimeException): TestResult {
            statistics.fail()
            println(e.message)
            return TestResult(TestStatus.FAILED, "Exception: ${e.message}. Cause: ${e.cause?.message}")
        }

        open fun error(e: Exception): TestResult {
            statistics.error()
            return TestResult(TestStatus.ERROR, "Exception: ${e.message}. Cause: ${e.cause?.message}")
        }

        open fun skip(): TestResult {
            statistics.skip()
            return TestResult(TestStatus.SKIPPED)
        }
    }

    open fun createTestCase(name: String) = KonanTestCase(name)

    open fun start() {}
    open fun finish() {}
}

class TeamcityKonanTestSuite(name: String, statistics: Statistics) : KonanTestSuite(name, statistics) {

    inner class TeamcityKonanTestCase(name: String) : KonanTestCase(name) {
        private fun teamcityFinish() {
            teamcityReport("testFinished name='$name'")
        }

        override fun start() {
            teamcityReport("testStarted name='$name'")
        }

        override fun pass(): TestResult {
            teamcityFinish()
            return super.pass()
        }

        override fun fail(e: RuntimeException): TestResult {
            teamcityReport("testFailed type='comparisonFailure' name='$name' message='${toTeamCityFormat(e.message)}'")
            teamcityFinish()
            return super.fail(e)
        }

        override fun error(e: Exception): TestResult {
            val writer = StringWriter()
            e.printStackTrace(PrintWriter(writer))
            val rawString  = writer.toString()

            teamcityReport("testFailed name='$name' message='${toTeamCityFormat(e.message)}' " +
                    "details='${toTeamCityFormat(rawString)}'")
            teamcityFinish()
            return super.error(e)
        }

        override fun skip(): TestResult {
            teamcityReport("testIgnored name='$name'")
            teamcityFinish()
            return super.skip()
        }
    }

    override fun createTestCase(name: String) = TeamcityKonanTestCase(name)

    private fun teamcityReport(msg: String) {
        println("##teamcity[$msg]")
    }

    /**
     * Teamcity require escaping some symbols in pipe manner.
     * https://github.com/GitTools/GitVersion/issues/94
     */
    fun toTeamCityFormat(inStr: String?): String = inStr?.let {
        it.replace("\\|", "||")
                .replace("\r", "|r")
                .replace("\n", "|n")
                .replace("'", "|'")
                .replace("\\[", "|[")
                .replace("]", "|]")
    } ?: "null"

    override fun start() {
        teamcityReport("testSuiteStarted name='$name'")
    }

    override fun finish() {
        teamcityReport("testSuiteFinished name='$name'")
    }
}
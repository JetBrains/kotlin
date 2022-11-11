@file:Suppress("PackageDirectoryMismatch", "unused")

package abitestutils

import abitestutils.ThrowableKind.*

/** API **/

interface TestBuilder {
    fun skipHashes(message: String): ErrorMessagePattern
    fun nonImplementedCallable(callableTypeAndName: String, classifierTypeAndName: String): ErrorMessagePattern

    fun expectFailure(errorMessagePattern: ErrorMessagePattern, block: Block<Any?>)
    fun expectSuccess(block: Block<String>) // OK is expected
    fun <T : Any> expectSuccess(expectedOutcome: T, block: Block<T>)
}

sealed interface ErrorMessagePattern

private typealias Block<T> = () -> T

fun abiTest(init: TestBuilder.() -> Unit): String {
    val builder = TestBuilderImpl()
    builder.init()
    builder.check()
    return builder.runTests()
}

/** Implementation **/

private const val OK_STATUS = "OK"

private class TestBuilderImpl : TestBuilder {
    private val tests = mutableListOf<Test>()

    override fun skipHashes(message: String) = ErrorMessageWithSkippedSignatureHashes(message)

    override fun nonImplementedCallable(callableTypeAndName: String, classifierTypeAndName: String) =
        NonImplementedCallable(callableTypeAndName, classifierTypeAndName)

    override fun expectFailure(errorMessagePattern: ErrorMessagePattern, block: Block<Any?>) {
        tests += FailingTest(errorMessagePattern, block)
    }

    override fun expectSuccess(block: Block<String>) = expectSuccess(OK_STATUS, block)

    override fun <T : Any> expectSuccess(expectedOutcome: T, block: Block<T>) {
        tests += SuccessfulTest(expectedOutcome, block)
    }

    fun check() {
        check(tests.isNotEmpty()) { "No ABI tests configured" }
    }

    fun runTests(): String {
        val testFailures: List<TestFailure> = tests.mapIndexedNotNull { serialNumber, test ->
            val testFailureDetails: TestFailureDetails? = when (test) {
                is FailingTest -> {
                    try {
                        test.block()
                        TestSuccessfulButMustFail
                    } catch (t: Throwable) {
                        when (t.throwableKind) {
                            IR_LINKAGE_ERROR -> test.checkIrLinkageErrorMessage(t)
                            EXCEPTION -> TestFailedWithException(t)
                            NON_EXCEPTION -> throw t // Something totally unexpected. Rethrow.
                        }
                    }
                }

                is SuccessfulTest -> {
                    try {
                        val result = test.block()
                        if (result == test.expectedOutcome)
                            null // Success.
                        else
                            TestMismatchedExpectation(test.expectedOutcome, result)
                    } catch (t: Throwable) {
                        if (t.throwableKind == NON_EXCEPTION)
                            throw t // Something totally unexpected. Rethrow.
                        else
                            TestFailedWithException(t)
                    }
                }
            }

            if (testFailureDetails != null) TestFailure(serialNumber, testFailureDetails) else null
        }

        return if (testFailures.isEmpty()) OK_STATUS else testFailures.joinToString(prefix = "\n", separator = "\n", postfix = "\n")
    }
}

private sealed interface AbstractErrorMessagePattern : ErrorMessagePattern {
    fun checkIrLinkageErrorMessage(errorMessage: String?): TestFailureDetails?
}

private class ErrorMessageWithSkippedSignatureHashes(private val expectedMessage: String) : AbstractErrorMessagePattern {
    init {
        check(expectedMessage.isNotBlank()) { "Message is blank: [$expectedMessage]" }
    }

    override fun checkIrLinkageErrorMessage(errorMessage: String?) =
        if (errorMessage?.replace(SIGNATURE_WITH_HASH) { it.groupValues[1] } == expectedMessage)
            null // Success.
        else
            TestMismatchedExpectation(expectedMessage, errorMessage)

    companion object {
        val SIGNATURE_WITH_HASH = Regex("(symbol /[\\da-zA-Z.<>_\\-]+)(\\|\\S+)")
    }
}

private class NonImplementedCallable(callableTypeAndName: String, classifierTypeAndName: String) : AbstractErrorMessagePattern {
    init {
        check(callableTypeAndName.isNotBlank() && ' ' in callableTypeAndName) { "Invalid callable type & name: [$callableTypeAndName]" }
        check(classifierTypeAndName.isNotBlank() && ' ' in classifierTypeAndName) { "Invalid classifier type & name: [$classifierTypeAndName]" }
    }

    private val fullMessage = "Abstract $callableTypeAndName is not implemented in non-abstract $classifierTypeAndName"

    override fun checkIrLinkageErrorMessage(errorMessage: String?) =
        if (errorMessage == fullMessage)
            null // Success.
        else
            TestMismatchedExpectation(fullMessage, errorMessage)
}

private sealed interface Test
private class FailingTest(val errorMessagePattern: ErrorMessagePattern, val block: Block<Any?>) : Test
private class SuccessfulTest(val expectedOutcome: Any, val block: Block<Any>) : Test

private class TestFailure(val serialNumber: Int, val details: TestFailureDetails) {
    override fun toString() = "#$serialNumber: ${details.description}"
}

private sealed class TestFailureDetails(val description: String)
private object TestSuccessfulButMustFail : TestFailureDetails("Test is successful but was expected to fail.")
private class TestFailedWithException(t: Throwable) : TestFailureDetails("Test unexpectedly failed with exception: $t")
private class TestMismatchedExpectation(expectedOutcome: Any, actualOutcome: Any?) :
    TestFailureDetails("EXPECTED: $expectedOutcome, ACTUAL: $actualOutcome")

private enum class ThrowableKind { IR_LINKAGE_ERROR, EXCEPTION, NON_EXCEPTION }

private val Throwable.throwableKind: ThrowableKind
    get() = when {
        this::class.simpleName == "IrLinkageError" -> IR_LINKAGE_ERROR
        this is Exception -> EXCEPTION
        else -> NON_EXCEPTION
    }

private fun FailingTest.checkIrLinkageErrorMessage(t: Throwable) =
    (errorMessagePattern as AbstractErrorMessagePattern).checkIrLinkageErrorMessage(t.message)

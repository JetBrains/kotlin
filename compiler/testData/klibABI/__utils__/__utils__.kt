@file:Suppress("PackageDirectoryMismatch", "unused")

package abitestutils

import abitestutils.TestMode.*

/** Public API **/

interface TestBuilder {
    val testMode: TestMode

    /** N.B. It is expected that [messageWithoutHashes] contains IR linkage error message without hashes in ID signatures. */
    fun linkage(messageWithoutHashes: String): FailurePattern
    fun nonImplementedCallable(callableTypeAndName: String, classifierTypeAndName: String): FailurePattern
    fun noWhenBranch(): FailurePattern
    fun custom(checker: (Throwable) -> Boolean): FailurePattern

    fun expectFailure(failurePattern: FailurePattern, block: Block<Any?>)
    fun expectSuccess(block: Block<String>) // OK is expected
    fun <T : Any> expectSuccess(expectedOutcome: T, block: Block<T>)
}

sealed interface FailurePattern

private typealias Block<T> = () -> T

enum class TestMode {
    JS_NO_IC,
    JS_WITH_IC,
    NATIVE_CACHE_NO,
    NATIVE_CACHE_STATIC_ONLY_DIST,
    NATIVE_CACHE_STATIC_EVERYWHERE,
}

fun abiTest(init: TestBuilder.() -> Unit): String {
    val builder = TestBuilderImpl()
    builder.init()
    builder.check()
    return builder.runTests()
}

/** Implementation **/

private const val OK_STATUS = "OK"

private class TestBuilderImpl : TestBuilder {
    override val testMode = TestMode.__UNKNOWN__

    private val tests = mutableListOf<Test>()

    override fun linkage(messageWithoutHashes: String) = GeneralIrLinkageError(messageWithoutHashes)

    override fun nonImplementedCallable(callableTypeAndName: String, classifierTypeAndName: String) =
        NonImplementedCallableIrLinkageError(callableTypeAndName, classifierTypeAndName)

    override fun noWhenBranch() = NoWhenBranchFailure
    override fun custom(checker: (Throwable) -> Boolean) = CustomThrowableFailure(checker)

    override fun expectFailure(failurePattern: FailurePattern, block: Block<Any?>) {
        tests += FailingTest(failurePattern as AbstractFailurePattern, block)
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
                        test.failurePattern.validateFailure(t)
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
                        TestFailedWithException(t)
                    }
                }
            }

            if (testFailureDetails != null) TestFailure(serialNumber, test.sourceLocation, testFailureDetails) else null
        }

        return if (testFailures.isEmpty()) OK_STATUS else testFailures.joinToString(prefix = "\n", separator = "\n", postfix = "\n")
    }
}

private sealed interface AbstractFailurePattern : FailurePattern {
    fun validateFailure(t: Throwable): TestFailureDetails?
}

private sealed class AbstractIrLinkageErrorPattern : AbstractFailurePattern {
    final override fun validateFailure(t: Throwable) =
        if (t.isLinkageError)
            checkIrLinkageErrorMessage(t.message) // OK, this is IR linkage error. Validate the message.
        else
            TestFailedWithException(t) // Unexpected type of exception.

    abstract fun checkIrLinkageErrorMessage(errorMessage: String?): TestFailureDetails?
}

private class GeneralIrLinkageError(private val expectedMessageWithoutHashes: String) : AbstractIrLinkageErrorPattern() {
    init {
        check(expectedMessageWithoutHashes.isNotBlank()) { "Message is blank: [$expectedMessageWithoutHashes]" }
    }

    override fun checkIrLinkageErrorMessage(errorMessage: String?) =
        if (errorMessage?.replace(SIGNATURE_WITH_HASH) { it.groupValues[1] + "'" } == expectedMessageWithoutHashes)
            null // Success.
        else
            TestMismatchedExpectation(expectedMessageWithoutHashes, errorMessage)

    companion object {
        val SIGNATURE_WITH_HASH = Regex("(symbol '/[\\da-zA-Z.<>_\\-]+)(\\|\\S+)'")
    }
}

private class NonImplementedCallableIrLinkageError(
    callableTypeAndName: String,
    classifierTypeAndName: String
) : AbstractIrLinkageErrorPattern() {
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

private class CustomThrowableFailure(private val checker: (Throwable) -> Boolean) : AbstractFailurePattern {
    override fun validateFailure(t: Throwable) =
        if (checker(t))
            null // Expected failure.
        else
            TestFailedWithException(t) // Unexpected type of exception.
}

@Suppress("PrivatePropertyName")
private val NoWhenBranchFailure = CustomThrowableFailure { it is NoWhenBranchMatchedException }

private sealed class Test {
    val sourceLocation: String? = computeSourceLocation()
}

private class FailingTest(val failurePattern: AbstractFailurePattern, val block: Block<Any?>) : Test()
private class SuccessfulTest(val expectedOutcome: Any, val block: Block<Any>) : Test()

private class TestFailure(val serialNumber: Int, val sourceLocation: String?, val details: TestFailureDetails) {
    override fun toString() = buildString {
        append('#').append(serialNumber)
        if (sourceLocation != null) append(" (").append(sourceLocation).append(")")
        append(": ").append(details.description)
    }
}

private sealed class TestFailureDetails(val description: String)
private object TestSuccessfulButMustFail : TestFailureDetails("Test is successful but was expected to fail.")
private class TestFailedWithException(t: Throwable) : TestFailureDetails("Test unexpectedly failed with exception: $t")
private class TestMismatchedExpectation(expectedOutcome: Any, actualOutcome: Any?) :
    TestFailureDetails("EXPECTED: $expectedOutcome, ACTUAL: $actualOutcome")

private val Throwable.isLinkageError: Boolean
    get() = this::class.simpleName == "IrLinkageError"

fun computeSourceLocation(): String? {
    fun extractSourceLocation(stackTraceLine: String): String? {
        return stackTraceLine.substringAfterLast('(', missingDelimiterValue = "")
            .substringBefore(')', missingDelimiterValue = "")
            .takeIf { it.isNotEmpty() }
            ?.split(':', limit = 2)
            ?.takeIf { it.size == 2 && it[0].isNotEmpty() && it[1].isNotEmpty() }
            ?.let { "${it[0].substringAfterLast('/').substringAfterLast('\\')}:${it[1]}" }
    }

    var beenInTestBuilderImpl = false

    // Capture the stack trace to find out the line number where the test was exactly configured.
    return Throwable().stackTraceToString()
        .lineSequence()
        .dropWhile { stackTraceLine ->
            val isInTestBuilderImpl = TestBuilderImpl::class.simpleName!! in stackTraceLine
            if (isInTestBuilderImpl) {
                beenInTestBuilderImpl = true
                true
            } else {
                !beenInTestBuilderImpl
            }
        }
        .mapNotNull(::extractSourceLocation)
        .firstOrNull()
}

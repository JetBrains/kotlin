// KT-78707, KT-78998
// WITH_STDLIB
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-JS:2.4
// ^^^ KT-78707, KT-78998 are fixed in 2.5.0-Beta1

// TARGET_BACKEND: WASM
// WASM_STANDALONE
// ^^^ in non-standalone run, test classes will be placed in a sub-package, so `Throwable.toString()` would give different result

package a

class MyException(message: String) : RuntimeException(message)

// "    ... and 3 more common stack frames skipped" - replaces the frames a throwable shares with the trace it is
// printed inside of. How many are shared is engine-specific, so only the presence of the line is comparable.
private fun isCommonFramesLine(line: String): Boolean = line.trimStart().startsWith("... and ")

// Frame lines look different on every target and engine, so they are only recognized, never compared:
//   V8:               "    at <module>.a.box (wasm://wasm/...)"
//   SpiderMonkey/JSC: "a.box@wasm://wasm/..."
private fun isFrameLine(line: String): Boolean {
    val trimmed = line.trimStart()
    return trimmed.startsWith("at ") || isCommonFramesLine(line) || '@' in trimmed
}

// Everything in a trace except the frames themselves. WASI has no stack frames at all, and how many frames are
// dropped as common with the enclosing trace differs per target, so only these lines are comparable.
private fun headerLines(trace: String): List<String> =
    trace.lines().filter { it.isNotEmpty() && !isFrameLine(it) }

// A stack trace must start with header lines, equal to Throwable.toString().
// Stack trace header can also contain engine specific header - we don't check it.
private fun checkTrace(e: Throwable, expectedHeader: String): String? {
    val actualHeader = e.toString()
    if (actualHeader != expectedHeader)
        return "toString(): expected <$expectedHeader>, got <$actualHeader>"

    val trace = e.stackTraceToString()
    // Parsing stack trace until stack frames provided by the engine.
    // Check that the `header` is exactly `expectedHeader` - e.g. no additional lines in stack trace header,
    // like repetitions of Throwable.toString().
    val header = trace.lines().takeWhile { it.isNotEmpty() && !isFrameLine(it) }
    if (header != expectedHeader.lines())
        return "stackTraceToString() header: expected [<$expectedHeader>], got $header\nfull trace:\n$trace"

    return null
}

fun testTraces(): String? {

    val withoutMessage = try { throw RuntimeException() } catch (e: Throwable) { e }
    checkTrace(withoutMessage, "kotlin.RuntimeException")?.let { return it }

    // on JVM also has `java.lang.RuntimeException: `
    val withEmptyMessage = try { throw RuntimeException("") } catch (e: Throwable) { e }
    checkTrace(withEmptyMessage, "kotlin.RuntimeException: ")?.let { return it }

    val withMessage = try { throw RuntimeException("some error") } catch (e: Throwable) { e }
    checkTrace(withMessage, "kotlin.RuntimeException: some error")?.let { return it }

    val multiLineMessage = try { throw RuntimeException("first line\nsecond line") } catch (e: Throwable) { e }
    checkTrace(multiLineMessage, "kotlin.RuntimeException: first line\nsecond line")?.let { return it }

    val custom = try { throw MyException("some error") } catch (e: Throwable) { e }
    checkTrace(custom, "a.MyException: some error")?.let { return it }

    return null
}

// Checks how suppressed exceptions and the cause chain are laid out around the frames.
private fun checkStructure(e: Throwable, expected: List<String>): String? {
    val trace = e.stackTraceToString()
    val actual = headerLines(trace)
    if (actual != expected)
        return "stackTraceToString() structure:\nexpected $expected\ngot      $actual\nfull trace:\n$trace"

    return null
}

fun testSuppressionStructure(): String? {

    // Suppressed exceptions are printed after the frames of the throwable they were suppressed by, indented by
    // four spaces, and before its cause chain.
    val wrapper = try {
        try {
            throw IllegalArgumentException("root cause")
        } catch (cause: Throwable) {
            throw RuntimeException("wrapper", cause)
        }
    } catch (e: Throwable) { e }
    wrapper.addSuppressed(IllegalStateException("first suppressed"))
    wrapper.addSuppressed(IllegalStateException("second suppressed"))

    checkStructure(
        wrapper,
        listOf(
            "kotlin.RuntimeException: wrapper",
            "    Suppressed: kotlin.IllegalStateException: first suppressed",
            "    Suppressed: kotlin.IllegalStateException: second suppressed",
            "Caused by: kotlin.IllegalArgumentException: root cause",
        )
    )?.let { return it }

    // A throwable that is reachable twice must be reported as a circular reference instead of being dumped again.
    val e1 = try { throw RuntimeException("e1") } catch (e: Throwable) { e }
    val e2 = try { throw RuntimeException("e2") } catch (e: Throwable) { e }
    e1.addSuppressed(e2)
    e2.addSuppressed(e1)

    checkStructure(
        e1,
        listOf(
            "kotlin.RuntimeException: e1",
            "    Suppressed: kotlin.RuntimeException: e2",
            "        Suppressed: [CIRCULAR REFERENCE, SEE ABOVE: kotlin.RuntimeException: e1]",
        )
    )?.let { return it }

    return null
}

fun box(): String {

    testTraces()?.let { return it }

    testSuppressionStructure()?.let { return it }

    return "OK"
}

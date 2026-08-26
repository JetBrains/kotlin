// KT-78707, KT-78998
// TODO KT-88517 [Wasm]: make JsException name/message more clear
// TARGET_BACKEND: WASM_JS
// WITH_STDLIB

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

private fun hasFrames(trace: String): Boolean =
    trace.lines().any { isFrameLine(it) && !isCommonFramesLine(it) }

// Everything in a trace except the frames themselves.
private fun headerLines(trace: String): List<String> =
    trace.lines().filter { it.isNotEmpty() && !isFrameLine(it) }

fun throwJsExceptionWithMessage(): Int = js("{ throw new TypeError('Test'); }")

fun throwJsExceptionWithEmptyMessage(): Int = js("{ throw new TypeError(); }")

fun throwJsExceptionWithNull(): Int = js("{ throw new TypeError(null); }")

fun throwJsExceptionWithMultilineMessage(): Int = js("{ throw new Error('first\\nsecond'); }")

fun throwJsExceptionWithCustomName(): Int = js("{ const e = new Error('Test'); e.name = 'CustomName'; throw e; }")

// ECMA-262 leaves just the message when the name is empty, so there is no leading colon.
fun throwJsExceptionWithEmptyName(): Int = js("{ const e = new Error('Test'); e.name = ''; throw e; }")

fun throwJsExceptionSubclass(): Int = js("{ class MyError extends Error {}; throw new MyError('Test'); }")

fun throwJsExceptionWithCause(): Int = js("{ throw new Error('outer', { cause: new Error('inner') }); }")

fun throwJsExceptionNull(): Int = js("{ throw null; }")

fun throwJsExceptionString(): Int = js("{ throw 'Test'; }")

fun throwJsExceptionNumber(): Int = js("{ throw 42; }")

fun throwJsExceptionPlainObject(): Int = js("{ throw { message: 'Test' }; }")

private const val NOT_AN_ERROR_MESSAGE = "Exception was thrown while running JavaScript code"
private const val NOT_AN_ERROR = "kotlin.js.JsException: $NOT_AN_ERROR_MESSAGE"

private fun catchException(block: () -> Unit): Throwable? =
    try {
        block()
        null
    } catch (e: Throwable) {
        e
    }

private fun checkJsException(
    block: () -> Unit,
    expectedToString: String,
    expectedMessage: String?,
    expectFrames: Boolean,
): String? {
    val e = catchException(block) ?: return "nothing was thrown, expected <$expectedToString>"
    if (e !is JsException) return "expected a JsException, got <${e::class.simpleName}>"

    if (e.toString() != expectedToString)
        return "toString(): expected <$expectedToString>, got <$e>"
    if (e.message != expectedMessage)
        return "message: expected <$expectedMessage>, got <${e.message}>"

    val trace = e.stackTraceToString()
    if (!trace.startsWith(expectedToString))
        return "trace header: expected to start with <$expectedToString>\ngot trace:\n$trace"
    if (hasFrames(trace) != expectFrames)
        return "expected frames=$expectFrames\nfull trace:\n$trace"

    return null
}

fun box(): String {

    checkJsException(
        ::throwJsExceptionWithMessage,
        expectedToString = "kotlin.js.JsException: Test",
        expectedMessage = "Test",
        expectFrames = true,
    )?.let { return it }

    checkJsException(
        ::throwJsExceptionWithEmptyMessage,
        expectedToString = "kotlin.js.JsException: ",
        expectedMessage = "",
        expectFrames = true,
    )?.let { return it }

    // `new TypeError(null)` stringifies its argument, so the message is the four characters \"null\".
    checkJsException(
        ::throwJsExceptionWithNull,
        expectedToString = "kotlin.js.JsException: null",
        expectedMessage = "null",
        expectFrames = true,
    )?.let { return it }

    checkJsException(
        ::throwJsExceptionWithMultilineMessage,
        expectedToString = "kotlin.js.JsException: first\nsecond",
        expectedMessage = "first\nsecond",
        expectFrames = true,
    )?.let { return it }

    checkJsException(
        ::throwJsExceptionWithCustomName,
        expectedToString = "kotlin.js.JsException: Test",
        expectedMessage = "Test",
        expectFrames = true,
    )?.let { return it }

    checkJsException(
        ::throwJsExceptionWithEmptyName,
        expectedToString = "kotlin.js.JsException: Test",
        expectedMessage = "Test",
        expectFrames = true,
    )?.let { return it }

    checkJsException(
        ::throwJsExceptionSubclass,
        expectedToString = "kotlin.js.JsException: Test",
        expectedMessage = "Test",
        expectFrames = true,
    )?.let { return it }

    checkJsException(::throwJsExceptionNull, NOT_AN_ERROR, NOT_AN_ERROR_MESSAGE, expectFrames = false)?.let { return it }
    checkJsException(::throwJsExceptionString, NOT_AN_ERROR, NOT_AN_ERROR_MESSAGE, expectFrames = false)?.let { return it }
    checkJsException(::throwJsExceptionNumber, NOT_AN_ERROR, NOT_AN_ERROR_MESSAGE, expectFrames = false)?.let { return it }
    checkJsException(::throwJsExceptionPlainObject, NOT_AN_ERROR, NOT_AN_ERROR_MESSAGE, expectFrames = false)?.let { return it }

    return "OK"
}

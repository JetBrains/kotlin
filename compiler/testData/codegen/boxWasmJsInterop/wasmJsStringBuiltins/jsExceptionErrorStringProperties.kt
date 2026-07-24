// TARGET_BACKEND: WASM
// WITH_STDLIB

private const val EXPECTED = "A\u0000B\uD83D\uDE80"

fun throwJsError(): Nothing = js("{ throw new Error('A\\u0000B\\uD83D\\uDE80'); }")

@JsFun("""(error) => error.message""")
external fun jsErrorMessage(error: JsAny?): String

@JsFun("""(error) => typeof error.message === "string" && !(error.message instanceof String)""")
external fun jsErrorMessageIsPrimitive(error: JsAny?): Boolean

@JsFun("(error) => typeof error.stack === \"string\"")
external fun jsErrorStackIsString(error: JsAny?): Boolean

fun box(): String {
    try {
        throwJsError()
        return "Fail no exception"
    } catch (e: Throwable) {
        if (e !is JsException) return "Fail wrong exception: ${e::class.simpleName}"
        if (e.message != EXPECTED) return "Fail message: <${e.message}>"

        val thrownValue = e.thrownValue ?: return "Fail missing thrown value"
        if (jsErrorMessage(thrownValue) != EXPECTED) return "Fail JS message"
        if (!jsErrorMessageIsPrimitive(thrownValue)) return "Fail JS message is not primitive"
        if (!jsErrorStackIsString(thrownValue)) return "Fail JS stack is not string"

        val stack = e.stackTraceToString()
        if (!stack.contains("throwJsError")) return "Fail stack: <$stack>"
    }

    return "OK"
}

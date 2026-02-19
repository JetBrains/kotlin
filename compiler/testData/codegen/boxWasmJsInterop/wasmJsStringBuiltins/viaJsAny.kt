// TARGET_BACKEND: WASM
// WITH_STDLIB

@JsFun("(v) => v")
external fun jsIdentityAny(v: JsAny?): JsAny?

@JsFun("(v) => typeof v")
external fun jsTypeOf(v: JsAny?): String

@JsFun("(v) => String(v)")
external fun jsToString(v: JsAny?): String

fun box(): String {
    val cases = listOf(
        "",
        "ASCII",
        "🚀\u0000e\u0301",
        "Привет",
        "שלום"
    )

    for ((i, s) in cases.withIndex()) {
        val jsStr = s.toJsString()
        val any = jsIdentityAny(jsStr)

        if (any == null) return "Fail1"

        val type = jsTypeOf(any)
        if (type != "string") return "Fail2"

        val back = jsToString(any)
        if (back != s) return "Fail3"
    }

    return "OK"
}

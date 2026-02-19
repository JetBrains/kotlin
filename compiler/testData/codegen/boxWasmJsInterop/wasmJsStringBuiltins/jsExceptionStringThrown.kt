// TARGET_BACKEND: WASM
// WITH_STDLIB

private val EXPECTED = "🚀\u0000e\u0301"

fun throwJsString(): Nothing = js("{ throw '🚀\\u0000e\\u0301'; }")

@JsFun("""
(v) => {
  if (typeof v !== "string") return "typeof=" + (typeof v);
  return v;
}
""")
external fun jsVerifyAndEcho(v: JsAny?): String

fun box(): String {
    try {
        throwJsString()
        return "Fail1"
    } catch (e: Throwable) {
        if (e !is JsException) return "Fail2"
        val echoed = jsVerifyAndEcho(e.thrownValue)
        if (echoed != EXPECTED) return "Fail3"
        return "OK"
    }
}

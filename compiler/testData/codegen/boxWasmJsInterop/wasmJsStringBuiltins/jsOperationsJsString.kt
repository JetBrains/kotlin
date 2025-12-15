// TARGET_BACKEND: WASM
// WITH_STDLIB

fun jsUpper(s: JsString): JsString = js("s.toUpperCase()")
fun jsSlice1(s: JsString): JsString = js("s.slice(1)")
fun jsConcat(a: JsString, b: JsString): JsString = js("a + b")

fun jsIsPrimitiveString(s: JsString): Boolean = js("typeof s === 'string' && !(s instanceof String)")

fun box(): String {
    val s = "Привет🚀\u0000e\u0301"

    val jsS = s.toJsString()

    if (!jsIsPrimitiveString(jsS)) return "Fail1"

    val up = jsUpper(jsS).toString()
    if (up != "ПРИВЕТ🚀\u0000E\u0301") return "Fail2"

    val sl = jsSlice1(jsS).toString()
    if (sl != "ривет🚀\u0000e\u0301") return "Fail3"

    val joined = jsConcat("<<".toJsString(), jsS).toString()
    if (joined != "<<$s") return "Fail4"

    if (jsS.toString() != s) return "Fail5"

    return "OK"
}

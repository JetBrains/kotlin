// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^ In JS Wasm interop types are mapped to Kotlin types, so JsString is String; In Wasm all of them are different types

fun assertTrue(x: Boolean): Unit = check(x)
fun assertNull(x: Any?): Unit = check(x === null)

fun jsRepresentation(x: JsAny): String = js("(typeof x) + ':' + String(x)")

@Suppress("INCOMPATIBLE_TYPES", "IMPOSSIBLE_IS_CHECK_ERROR")
fun castToKotlinString(jsAny: JsAny?): String? =
    if (jsAny is String) jsAny as String else null

fun box(): String {
    // JsString
    val jsStr1: JsString = "str1".toJsString()
    assertNull(castToKotlinString(jsStr1))

    return "OK"
}
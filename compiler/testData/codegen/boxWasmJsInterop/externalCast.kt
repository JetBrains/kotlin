// TARGET_BACKEND: WASM
// MODULE: main

// FILE: test.kt

fun isJsString(obj: JsAny) : Boolean = obj is JsString

fun box(): String {
    if (!isJsString("foo".toJsString())) return "Fail for JsString"
    if (isJsString(1.toJsNumber())) return "Fail for JsNumber"

    return "OK"
}



// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: WASM
// TARGET_BACKEND: WASM
// WITH_STDLIB
// FILE: contextParametersJSModule.kt

external interface Context {
    fun action(): String
}

@JsModule("./contextParametersJSModule.mjs")
external object JS {
    fun get(): Context
}

context(c: Context)
fun test(): String = c.action()

fun usage1(c: Context): String = context(c) { test() }

fun usage2(c: Context): String = with(c) { test() }

context(c: Context)
fun usage3(): String = test()


fun box(): String {
    val ctx = JS.get()

    val r1 = usage1(ctx)
    val r2 = usage2(ctx)
    val r3 = context(ctx) { usage3() }

    return if (r1 == "OK" && r2 == "OK" && r3 == "OK") "OK"
    else "FAIL"
}
// FILE: contextParametersJSModule.mjs
export function get () {
    return {
        action () {
            return "OK";
        }
    };
}
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_STDLIB

fun foo(x : Any): String {
    return if(x is Array<*> && x.isArrayOf<String>()) (x as Array<String>)[0] else "fail"
}

fun box(): String {
    return foo(arrayOf("OK"))
}

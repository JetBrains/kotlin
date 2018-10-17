// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun foo(x : Any): String {
    return if(x is Array<*> && x.isArrayOf<String>()) (x as Array<String>)[0] else "fail"
}

fun box(): String {
    return foo(arrayOf("OK"))
}

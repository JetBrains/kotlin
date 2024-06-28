// TARGET_BACKEND: JVM
// WITH_STDLIB

fun foo(x : Any): String {
    return if(x is Array<*> && x.isArrayOf<String>()) (x as Array<String>)[0] else "fail"
}

fun box(): String {
    return foo(arrayOf("OK"))
}

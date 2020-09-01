// ERROR_POLICY: SYNTAX

// FILE: t.kt


fun bar(aa: Any, bb: Any, cc: Any) {
    <<;;;;;
    d8as9d89as
    ??????? ====----
}

fun foo() {
    bar(a(), b(), c())
    <<<<<,,,,>>>>>
    f()
}

fun a(): Any { storage += "a"; return storage }
fun b(): Any { storage += "b"; return storage }
fun c(): Any { storage += "c"; return storage }
fun f(): Any { storage += "FAIL"; return storage }

var storage = ""

// FILE: b.kt

fun box(): String {
    try {
        foo()
    } catch (e: IllegalStateException) {
        return if (storage == "abc") "OK" else "FAIL ABC"
    return "FAIL"
}
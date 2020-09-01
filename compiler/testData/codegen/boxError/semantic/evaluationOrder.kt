// ERROR_POLICY: SEMANTIC

// FILE: t.kt


fun bar(aa: Any, bb: Any, cc: Any) {
}

fun foo() {
    qux(a(), b(), c())
    beer()
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
}
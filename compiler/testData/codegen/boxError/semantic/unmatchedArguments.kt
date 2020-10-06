// ERROR_POLICY: SEMANTIC

// FILE: t.kt

var storage = ""

fun bar(a: String, b: String) { storage += a; storage += b; }

fun foo1() {
    bar("O", "K")
    bar("FAIL1")
}

fun foo2() {
    bar("FAIL2", "FAIL2", "FAIL2", "FAIL2")
}

// FILE: b.kt

fun box(): String {
    try {
        foo1()
    } catch (e: IllegalStateException) {
        try {
            foo2()
        } catch (e: IllegalStateException) {
            return storage
        }
    }
    return "FAIL"
}
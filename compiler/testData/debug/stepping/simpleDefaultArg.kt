
// FILE: test.kt
fun ifoo(ok: String = "OK"): String {
    return ok
}

fun box(): String {
    return ifoo()
}

// LINENUMBERS
// test.kt:8
// test.kt:3
// test.kt:4
// test.kt:3
// test.kt:8
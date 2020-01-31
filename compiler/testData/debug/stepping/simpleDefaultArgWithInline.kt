// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
inline fun ifoo(ok: String = "OK"): String {
    return ok
}

fun box(): String {
    return ifoo()
}
// TODO: IR Backend behaves like simpleDefaultArg: 8,3,4, _3_ ,8
// LINENUMBERS
// test.kt:8
// test.kt:3
// test.kt:4
// test.kt:8
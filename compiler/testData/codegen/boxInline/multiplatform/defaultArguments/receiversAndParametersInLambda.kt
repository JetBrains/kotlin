// !LANGUAGE: +MultiPlatformProjects
// NO_CHECK_LAMBDA_INLINING
// IGNORE_BACKEND: JVM_IR

// FILE: 1.kt

class C(val s1: String, val s2: String)

expect fun C.test(r1: () -> String = { s1 }, r2: () -> String = this::s2): String

actual inline fun C.test(r1: () -> String, r2: () -> String): String = r1() + r2()


expect class D {
    val s1: String
    val s2: String

    fun test(r1: () -> String = { s1 }, r2: () -> String = this::s2): String
}

actual class D(actual val s1: String, actual val s2: String) {
    actual inline fun test(r1: () -> String, r2: () -> String): String = r1() + r2()
}

// FILE: 2.kt

fun box(): String {
    if (C("O", "K").test() != "OK") return "Fail extension receiver"
    if (D("O", "K").test() != "OK") return "Fail dispatch receiver"
    return "OK"
}

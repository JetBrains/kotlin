// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

fun foo(x: String = "OK"): String = x

fun box(): String {
    val f: () -> String = ::foo
    return f()
}

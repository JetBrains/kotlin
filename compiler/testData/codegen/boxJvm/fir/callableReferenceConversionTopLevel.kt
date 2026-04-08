// TARGET_BACKEND: JVM_IR

fun foo(x: String = "OK"): String = x

fun box(): String {
    val f: () -> String = ::foo
    return f()
}

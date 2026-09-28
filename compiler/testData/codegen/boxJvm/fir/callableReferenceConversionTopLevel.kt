// TARGET_BACKEND: JVM

fun foo(x: String = "OK"): String = x

fun box(): String {
    val f: () -> String = ::foo
    return f()
}

// FILE: lib.kt
inline fun <T> with(x: T, a: T.() -> Unit) = x.a()

// FILE: main.kt
var log = ""

fun bar(): A {
    log += "foo;"
    return A()
}

class A {
    fun f() {
        log += "f;"
    }

    fun g() {
        log += "g;"
    }
}

fun box(): String {
    with(bar()) {
        f()
        g()
    }

    if (log != "foo;f;g;") return "fail: $log"

    return "OK"
}


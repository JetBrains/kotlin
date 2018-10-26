package foo

inline fun foo(f: () -> Unit) = f()

var ok = "Fail"

fun main() {
    foo {
        ok = "OK"
    }
}

fun box(): String = ok
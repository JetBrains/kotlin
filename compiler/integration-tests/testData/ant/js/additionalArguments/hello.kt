package foo

inline fun foo(f: () -> Unit) = f()

var ok = "Fail"

fun main(args : Array<String>) {
    foo {
        ok = "OK"
    }
}

fun box(): String = ok
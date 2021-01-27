// FILE: 1.kt

class A {
    inline fun foo() {}
}

// FILE: 2.kt

fun box(): String {
    A().foo()

    return "OK"
}

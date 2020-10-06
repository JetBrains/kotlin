// ERROR_POLICY: SEMANTIC

// FILE: t.kt

class A
class B

fun bar(): B = B()
fun foo(): A {
    return bar()
}

// FILE: b.kt

fun box(): String {
    try {
        foo()
    } catch (e: ClassCastException) {
        return "OK"
    }
    return "FAIL"
}
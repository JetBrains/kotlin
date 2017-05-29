sealed class A {
    class B : A()

    class C : A()
}

inline fun foo(): A = A.B()

fun box(): String {
    val a: A = foo()
    val b: Boolean
    when (a) {
        is A.B -> b = true
        is A.C -> b = false
    }
    return if (b) "OK" else "FAIL"
}

// 0 TABLESWITCH
// 0 LOOKUPSWITCH
// 0 ATHROW
// 0 INSTANCEOF
// 0 FAIL
// 0 POP
sealed class A {
    object B : A()

    class C : A()
}

fun foo(): A = A.C()

fun box(): String {
    val a: A = foo()
    val b: Boolean
    when (a) {
        A.B -> b = true
        is A.C -> b = false
    }
    return if (!b) "OK" else "FAIL"
}

// 0 TABLESWITCH
// 0 LOOKUPSWITCH
// 1 ATHROW

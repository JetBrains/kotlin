sealed class A {
    object B : A()

    class C : A()
}

fun box(): String {
    val a: A = A.C()
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

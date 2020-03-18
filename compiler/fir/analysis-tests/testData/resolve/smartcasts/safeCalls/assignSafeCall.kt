// !DUMP_CFG
// ----------------- Stable -----------------

class A {
    fun foo(): Int = 1

    val x: Int = 1

    fun bar() {}
}

fun test_1(a: A?) {
    val x = a?.x
    if (x != null) {
        a.bar() // Should be OK
    }
}

fun test_2(a: A?) {
    val x = a?.foo()
    if (x != null) {
        a.bar() // Should be OK
    }
}

fun test_3(x: Any?) {
    val a = x as? A ?: return
    a.foo() // Should be OK
    x.foo() // Should be OK
}

// ----------------- Unstable -----------------

interface B {
    fun foo(): Int

    val x: Int

    fun bar()
}

fun test_1(a: B?) {
    val x = a?.x
    if (x != null) {
        a.bar() // Should be OK
    }
}

fun test_2(a: B?) {
    val x = a?.foo()
    if (x != null) {
        a.bar() // Should be OK
    }
}

fun test_3(x: Any?) {
    val a = x as? B ?: return
    a.foo() // Should be OK
    x.foo() // Should be OK
}
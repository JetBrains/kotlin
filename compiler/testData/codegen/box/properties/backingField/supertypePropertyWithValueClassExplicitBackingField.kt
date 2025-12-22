// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: NATIVE
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields
// WORKS_WHEN_VALUE_CLASS

// WITH_STDLIB

interface I { fun call(): Int }

OPTIONAL_JVM_INLINE_ANNOTATION
value class V(val x: Int) : I { override fun call(): Int = x }

class A {
    val p: I
        field = V(1)

    fun foo(): Int = p.x
}

fun foo(i: I): Int = i.call()
fun <T : I> bar(t: T): Int = t.call()

fun baz(vararg xs: I): Int {
    var s = 0
    for (x in xs) s += x.call()
    return s
}

fun box(): String {
    val a = A()
    if (a.foo() != 1) return "FAIL1"

    val i: I = a.p
    if (foo(i) != 1) return "FAIL2"
    if (bar(i) != 1) return "FAIL3"

    val v = V(1)
    if (bar(v) != 1) return "FAIL4"

    val arr = arrayOf<I>(i, V(1))
    if (baz(i, V(1)) != 2) return "FAIL5"
    if (baz(*arr) != 2) return "FAIL6"

    return "OK"
}

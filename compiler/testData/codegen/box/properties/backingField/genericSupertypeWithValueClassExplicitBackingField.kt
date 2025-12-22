// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: NATIVE
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields
// WORKS_WHEN_VALUE_CLASS

// WITH_STDLIB

OPTIONAL_JVM_INLINE_ANNOTATION
value class V(val x: Int) : Comparable<Int> {
    override fun compareTo(other: Int): Int = x - other
}

class A {
    val p: Comparable<Int>
        field = V(1)

    fun foo(): Int = p.x
}

fun foo(c: Comparable<Int>): Int = c.compareTo(1)
fun <T : Comparable<Int>> bar(t: T): Int = t.compareTo(1)

fun box(): String {
    val a = A()
    if (a.foo() != 1) return "FAIL0"

    val c: Comparable<Int> = a.p
    if (foo(c) != 0) return "FAIL1"
    if (bar(c) != 0) return "FAIL2"

    val any: Any = c
    if (any !is V) return "FAIL3"
    val v = any as? V ?: return "FAIL4"
    if (v.x != 1) return "FAIL5"

    return "OK"
}

// IGNORE_BACKEND_K1: ANY
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields

// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

OPTIONAL_JVM_INLINE_ANNOTATION
value class V(val x: Int)

class A {
    val p: Any
        field = V(1)

    fun foo(): Int = p.x
}

fun box(): String {
    val a = A()
    if (a.foo() != 1) return "FAIL0"

    val x: Any = a.p

    val s1 = x.toString()
    val s2 = x.toString()
    if (s1 != s2) return "FAIL1"

    val h1 = x.hashCode()
    val h2 = x.hashCode()
    if (h1 != h2) return "FAIL2"

    if (x != V(1)) return "FAIL3"
    if (V(1) != x) return "FAIL4"

    return "OK"
}

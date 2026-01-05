// IGNORE_BACKEND_K1: ANY
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields

// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

interface I { fun call(): Int }

OPTIONAL_JVM_INLINE_ANNOTATION
value class V(val x: Int) : I { override fun call(): Int = x }

class A {
    val a: Any
        field = V(1)

    fun foo(): Int = a.x
    fun bar(): Int = a.call() + a.x
}

fun <T> echo(t: T): T = t

fun box(): String {
    val obj = A()

    if (obj.foo() != 1) return "FAIL1"
    if (obj.bar() != 2) return "FAIL2"

    val vv: V = echo(obj.a as V)
    val b: Any = vv

    if (b !is V) return "FAIL3"
    val v = b as V
    if (v.x != 1) return "FAIL4"
    if (v.call() != 1) return "FAIL5"

    val i = b as I
    if (i.call() != 1) return "FAIL6"

    return "OK"
}

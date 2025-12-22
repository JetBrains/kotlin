// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: NATIVE
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields

// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

interface I { fun call(): Int }

OPTIONAL_JVM_INLINE_ANNOTATION
value class V(val x: Int) : I { override fun call(): Int = x }

class A(f: Boolean) {
    val p: Any?
        field = if (f) V(1) else null

    fun foo(): Boolean = (p == null)
    fun bar(): V? = p as? V
    fun baz(): Int = (p as? I)?.call() ?: 0
}

fun box(): String {
    val n = A(false)
    if (!n.foo()) return "FAIL1"
    if (n.bar() != null) return "FAIL2"
    if (n.baz() != 0) return "FAIL3"
    if (n.p != null) return "FAIL4"

    val y = A(true)
    if (y.foo()) return "FAIL5"

    val v = y.bar() ?: return "FAIL6"
    if (v.x != 1) return "FAIL7"
    if (y.baz() != 1) return "FAIL8"

    val b: Any? = y.p
    if (b !is V) return "FAIL9"
    val v2 = b as? V ?: return "FAIL10"
    if (v2.x != 1) return "FAIL11"

    return "OK"
}

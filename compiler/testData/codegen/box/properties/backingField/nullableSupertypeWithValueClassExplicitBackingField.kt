// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ExplicitBackingFields

// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

interface I { fun call(): Int }

OPTIONAL_JVM_INLINE_ANNOTATION
value class V(val x: Int) : I { override fun call(): Int = x }

class A(f: Boolean) {
    val p: I?
        field = if (f) V(1) else null

    fun foo(): Boolean = (p == null)
    fun bar(): Int = p?.call() ?: 0
    fun baz(): Int = p?.x ?: -1
}

fun box(): String {
    val n = A(false)
    if (!n.foo()) return "FAIL1"
    if (n.bar() != 0) return "FAIL2"
    if (n.baz() != -1) return "FAIL3"
    if (n.p != null) return "FAIL4"

    val y = A(true)
    if (y.foo()) return "FAIL5"
    if (y.bar() != 1) return "FAIL6"
    if (y.baz() != 1) return "FAIL7"

    val b = y.p ?: return "FAIL8"

    val v1 = b as? V ?: return "FAIL9"
    if (v1.x != 1) return "FAIL10"

    if (b.call() != 1) return "FAIL11"

    return "OK"
}

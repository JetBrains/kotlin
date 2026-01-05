// IGNORE_BACKEND_K1: ANY
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
}

fun box(): String {
    val s1: I = A().p
    val s2: I = V(1)

    if (s1 != s2) return "FAIL1"

    val set = hashSetOf(s1)
    if (!set.contains(s2)) return "FAIL2"

    val map = hashMapOf<I, String>(s1 to "ok")
    if (map[s2] != "ok") return "FAIL3"

    return "OK"
}

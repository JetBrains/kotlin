// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: NATIVE
// WITH_STDLIB

interface I { fun call(): Int }

@Suppress("UNRESOLVED_REFERENCE")
@JvmInline
value class V(val x: Int) : I { override fun call(): Int = x }

class A {
    val p: I
        field = V(1)

    fun foo(): Int = p.x
}

fun box(): String {
    val a = A()
    if (a.foo() != 1) return "FAIL1"

    val fromI: I = a.p
    if (fromI !is V) return "FAIL2"
    val v1 = fromI as V
    if (v1.x != 1) return "FAIL3"

    val fromAny: Any = fromI
    if (fromAny !is V) return "FAIL4"
    val v2 = fromAny as V
    if (v2.x != 1) return "FAIL5"

    if (v1.call() != 1 || v2.call() != 1) return "FAIL6"

    val i1: I = v1
    val i2: I = v2
    if (i1.call() != 1 || i2.call() != 1) return "FAIL7"

    val v3 = fromAny as? V ?: return "FAIL8"
    if (v3.x != 1) return "FAIL9"

    return "OK"
}

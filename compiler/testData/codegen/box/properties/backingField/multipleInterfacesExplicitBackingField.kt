// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: NATIVE
// WITH_STDLIB

interface I1 { fun call(): Int }
interface I2 { fun call2(): Int }

@Suppress("UNRESOLVED_REFERENCE")
@JvmInline
value class V(val x: Int) : I1, I2 {
    override fun call(): Int = x
    override fun call2(): Int = x + 1
}

class A {
    val p: I1
        field = V(10)
}

fun box(): String {
    val s: I1 = A().p

    if (s !is V) return "FAIL1"
    val v = s as V

    val i2: I2 = v
    val r = v.call() + i2.call2()
    if (r != 21) return "FAIL2"

    return "OK"
}

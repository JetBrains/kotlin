fun foo(x: Int, y: Int, z: String): Int = js("x + y + Number(z)")

val x: String = js("typeof 10")

fun fooDefault(x: Int = 1, y: Int? = 2, z: String? = "3"): Int =
    js("x + y + Number(z)")

external interface EIA
external class EIB : EIA

val eia10: EIA = js("'10'")
val eib1000: EIB = js("'1000'")

fun <T : EIA> fooDefaultT(x: T? = null): T? =
    js("x")

fun fooVararg(i: Int, vararg x: Int): Int =
    js("x[i]")

fun box(): String {
    val res = foo(10, 20, z = "30")
    if (res != 60) return "Wrong foo: $res"
    if (x != "number") return "Wrong x: $x"

    if (fooDefault() != 6) return "Wrong fooDefault 1"
    if (fooDefault(x = 100, y = 200, z = "300") != 600) return "Wrong fooDefault 2"
    if (fooDefault(y = 200, z = "300") != 501) return "Wrong fooDefault 3"

    if (fooDefaultT<EIA>() != null) return "Wrong fooDefaultT 1"
    if (fooDefaultT<EIA>(eia10).toString() != "10") return "Wrong fooDefaultT 2"
    if (fooDefaultT<EIB>(eib1000).toString() != "1000") return "Wrong fooDefaultT 3"

    if (fooVararg(0, 1) != 1) return "Wrong fooVararg 1"
    if (fooVararg(2, 1, 2, 3) != 3) return "Wrong fooVararg 2"
    if (fooVararg(2, 1, *intArrayOf(2, 3)) != 3) return "Wrong fooVararg 3"

    return "OK"
}
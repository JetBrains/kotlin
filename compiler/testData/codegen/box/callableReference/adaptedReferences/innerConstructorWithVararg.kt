// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType

class Outer(val o: String) {
    inner class Inner1(val i: Int, vararg v: String) {
        val result = "I1" + o + i + if (v.size == 0) "E" else v[0]
    }

    inner class Inner2(val i: Int, vararg v: String = arrayOf("A")) {
        val result = "I2" + o + i + v[0]
    }
}

fun <T> use0(f: (Int) -> T) = f(11)
fun <T> use1(f: (Int, String) -> T) = f(12, "B")

fun box(): String {
    val oouter = Outer("O")

    val r1 = use0(oouter::Inner1).result
    if (r1 != "I1O11E") return "Fail1: $r1"

    val r2 = use1(oouter::Inner1).result
    if (r2 != "I1O12B") return "Fail2: $r2"

    val r3 = use0(oouter::Inner2).result
    if (r3 != "I2O11A") return "Fail3: $r3"

    val r4 = use1(oouter::Inner2).result
    if (r4 != "I2O12B") return "Fail4: $r4"

    return "OK"
}

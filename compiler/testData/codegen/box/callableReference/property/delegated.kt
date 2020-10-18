// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
import kotlin.reflect.KProperty

val four: Int by NumberDecrypter

class A {
    val two: Int by NumberDecrypter
}

object NumberDecrypter {
    operator fun getValue(instance: Any?, data: KProperty<*>) = when (data.name) {
        "four" -> 4
        "two" -> 2
        else -> throw AssertionError()
    }
}

fun box(): String {
    val x = ::four.get()
    if (x != 4) return "Fail x: $x"
    val a = A()
    val y = A::two.get(a)
    if (y != 2) return "Fail y: $y"
    return "OK"
}

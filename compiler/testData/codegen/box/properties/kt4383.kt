import kotlin.reflect.KProperty

class D {
    operator fun getValue(a: Any, p: KProperty<*>) { }
}

object P {
    val u = Unit
    val v by D()
    var w = Unit
}

fun box(): String {
    if (P.u != P.v) return "Fail uv"
    P.w = Unit
    if (P.w != P.u) return "Fail w"
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES

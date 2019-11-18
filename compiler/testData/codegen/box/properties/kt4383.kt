// IGNORE_BACKEND_FIR: JVM_IR
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

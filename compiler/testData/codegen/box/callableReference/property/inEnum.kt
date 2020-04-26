
import kotlin.reflect.KProperty1

class Q {
  val s = "OK"
}

enum class PropEnum(val prop: KProperty1<Q, String>) {
    ELEM(Q::s)
}

fun box() = PropEnum.ELEM.prop.get(Q())

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES

// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES

import kotlin.reflect.KProperty1

class Q {
  val s = "OK"
}

enum class PropEnum(val prop: KProperty1<Q, String>) {
    ELEM(Q::s)
}

fun box() = PropEnum.ELEM.prop.get(Q())

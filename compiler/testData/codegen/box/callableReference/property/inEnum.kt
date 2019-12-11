// IGNORE_BACKEND_FIR: JVM_IR

import kotlin.reflect.KProperty1

class Q {
  val s = "OK"
}

enum class PropEnum(val prop: KProperty1<Q, String>) {
    ELEM(Q::s)
}

fun box() = PropEnum.ELEM.prop.get(Q())

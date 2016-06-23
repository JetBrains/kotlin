package delegatedPropertyInOtherFileOther

import kotlin.reflect.KProperty

class WithDelegate {
    val a: Int by Id(12)
}

class Id(val v: Int) {
    operator fun getValue(o: Any, property: KProperty<*>): Int = v
}

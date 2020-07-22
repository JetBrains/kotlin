// !LANGUAGE: +ProhibitSmartcastsOnLocalDelegatedProperty

class AlternatingDelegate {
    var counter: Int = 0
    operator fun getValue(thisRef: Any?, property: <!OTHER_ERROR!>KProperty<*><!>): Any? =
    if (counter++ % 2 == 0) 42 else ""
}

fun failsWithClassCastException() {
    val sometimesNotInt: Any? by AlternatingDelegate()

    if (sometimesNotInt is Int) {
        sometimesNotInt.inc()
    }
}
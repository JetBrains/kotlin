// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class StringDelegate {
    private var value: String = "unset"
    operator fun getValue(thisRef: Any?, p: KProperty<*>) = value
    operator fun setValue(thisRef: Any?, p: KProperty<*>, v: String) { value = v }
}

class ComplexProps {
    val cComputedProp: Int get() = 2
    val cLazyProp: String by lazy { "lazyVal" }
    var cObservedProp: Int by Delegates.observable(1) { _, old, new -> lastChange = "$old->$new" }
    var cDelegatedProp: String by StringDelegate()
    var lastChange = ""
}

fun box(): String {
    val o = ComplexProps()

    val (cComputedProp, cLazyProp, observedSnapshot = cObservedProp, delegatedSnapshot = cDelegatedProp) = o

    if (cComputedProp != 2) return "FAIL"
    if (cLazyProp != "lazyVal") return "FAIL"
    if (observedSnapshot != 1) return "FAIL"
    if (delegatedSnapshot != "unset") return "FAIL"

    o.cObservedProp = 10

    if (observedSnapshot != 1) return "FAIL"

    return "OK"
}

// !LANGUAGE: +NewInference
// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-31679

import kotlin.reflect.KProperty

class MyDelegate<T>(p: () -> T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = TODO()
}

private val privateObj by MyDelegate {
    object {
        val x = 42
    }
}

fun test() {
    privateObj.<!UNRESOLVED_REFERENCE!>x<!>
}

// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages

import kotlin.reflect.KProperty

class Delegate() {
    fun getValue(thisRef: Any?, property: KProperty<*>): String = ":)"
    fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }

    fun <caret>propertyDelegated(property: KProperty<*>) {
    }
}

var p: String by Delegate()

// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

import kotlin.reflect.KProperty

class Delegate() {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = ":)"

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }

    operator fun <caret>propertyDelegated(property: KProperty<*>) {
    }
}

var p: String by Delegate()

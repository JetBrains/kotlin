// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

import kotlin.reflect.KProperty

class Delegate() {
    fun getValue(thisRef: Any?, property: KProperty<*>): String = ":)"
    fun <caret>setValue(thisRef: Any?, property: KProperty<*>, value: String) {

    }
}

var p: String by Delegate()

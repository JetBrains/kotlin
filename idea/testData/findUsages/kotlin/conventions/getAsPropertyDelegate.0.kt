// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages

import kotlin.reflect.KProperty

class Delegate() {
    fun <caret>getValue(thisRef: Any?, property: KProperty<*>): String = ":)"
}

val p: String by Delegate()

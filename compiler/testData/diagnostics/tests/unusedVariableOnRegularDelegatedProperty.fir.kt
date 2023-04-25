// !DIAGNOSTICS: +UNUSED_VARIABLE

import kotlin.reflect.KProperty

class Example {
    val valProp: String by Delegate()
    val varProp: String by Delegate()

    fun foo() {
        val valVariable by Delegate()
        val varVariable by Delegate()
    }
}

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = "delegation"

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        // setValue
    }
}

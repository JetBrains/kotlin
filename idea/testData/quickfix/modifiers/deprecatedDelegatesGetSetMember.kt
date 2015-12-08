// "Rename to 'setValue'" "true"
// ERROR: 'get' method convention on type 'CustomDelegate' is no longer supported. Rename to 'getValue'

import kotlin.reflect.KProperty

class CustomDelegate {
    operator fun get(thisRef: Any?, prop: KProperty<*>): String = ""
    operator fun set(thisRef: Any?, prop: KProperty<*>, value: String) {}
}

class Example {
    var a: String <caret>by CustomDelegate()
}

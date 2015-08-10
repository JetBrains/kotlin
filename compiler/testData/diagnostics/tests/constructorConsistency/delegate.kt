// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class Delegate(val x: Int) {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = x
}

class My {
    val x: Int by Delegate(this.foo())

    fun foo(): Int = x
}

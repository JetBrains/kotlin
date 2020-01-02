// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A {
    val c: Int by Delegate()
}

class Delegate {
    fun getValue(t: Int, p: KProperty<*>): Int {
        return 1
    }

    fun getValue(t: String, p: KProperty<*>): Int {
        return 1
    }
}

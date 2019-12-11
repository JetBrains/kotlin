// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

import kotlin.reflect.KProperty

class Local {
    fun foo() {
        val a: Int by Delegate()
    }
}

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
        return 1
    }
}
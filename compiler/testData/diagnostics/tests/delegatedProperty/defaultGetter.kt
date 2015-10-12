// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val a: Int by Delegate()
    get

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
        return 1
    }
}

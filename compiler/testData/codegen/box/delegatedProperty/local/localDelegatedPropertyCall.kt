// WITH_STDLIB
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME
// DUMP_IR_AFTER_INLINE

import kotlin.reflect.*


class Delegate() {
    operator fun getValue(thiz: Any?, prop: KProperty<*>) : String {
        return (prop as KProperty0<*>).get() as String
    }
}

fun box() : String {
    return try {
        val x by Delegate()
        x
    } catch (e: UnsupportedOperationException) {
        "OK"
    }
}
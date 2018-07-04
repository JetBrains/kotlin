// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
package foo

import kotlin.reflect.KProperty

class Delegate {
    var inner = 1
    operator fun getValue(t: Any?, p: KProperty<*>): Int = inner
    operator fun setValue(t: Any?, p: KProperty<*>, i: Int) {
        inner = i
    }
}

fun box(): String {
    var prop: Int by Delegate()
    if (prop != 1) return "fail get"
    prop = 2
    if (prop != 2) return "fail set"
    return "OK"
}

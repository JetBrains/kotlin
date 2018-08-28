// IGNORE_BACKEND: JVM_IR
package foo

import kotlin.reflect.KProperty

fun <T> run(f: () -> T) = f()

class Delegate {
    var inner = 1
    operator fun getValue(t: Any?, p: KProperty<*>): Int = inner
    operator fun setValue(t: Any?, p: KProperty<*>, i: Int) {
        inner = i
    }
}

fun box(): String {
    var prop: Int by Delegate()
    run { prop = 2 }
    if (prop != 2) return "fail get"
    return run { if (prop != 2) "fail set" else "OK" }
}

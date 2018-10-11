// IGNORE_BACKEND: JVM_IR
package foo

import kotlin.reflect.KProperty

fun <T> run(f: () -> T) = f()

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

fun box(): String {
    val prop: Int by Delegate()
    return run { if (prop == 1) "OK" else "fail" }
}

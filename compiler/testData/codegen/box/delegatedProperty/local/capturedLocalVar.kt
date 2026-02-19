// FILE: lib.kt
package foo
inline fun <T> run(f: () -> T) = f()

// FILE: main.kt
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
    if (prop != 1) return "fail get 1"
    run { prop = 2 }
    if (prop != 2) return "fail get 2"
    return run { if (prop != 2) "fail get 3" else "OK" }
}

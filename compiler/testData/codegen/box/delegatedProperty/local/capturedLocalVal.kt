// FILE: lib.kt
package foo
inline fun <T> run(f: () -> T) = f()

// FILE: main.kt
package foo
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

fun box(): String {
    val prop: Int by Delegate()
    return run { if (prop == 1) "OK" else "fail" }
}

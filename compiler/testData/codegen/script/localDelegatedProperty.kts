
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 3
}

fun foo(): Int {
    val prop: Int by Delegate()
    return prop
}

val x = foo()

// expected: x: 3

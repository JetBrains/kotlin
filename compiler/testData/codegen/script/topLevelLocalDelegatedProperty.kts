
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 3
}


val prop: Int by Delegate()

val  x = prop

// expected: x: 3

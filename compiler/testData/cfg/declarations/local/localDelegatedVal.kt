import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

fun foo(): Int {
    val prop: Int by Delegate()
    return prop
}

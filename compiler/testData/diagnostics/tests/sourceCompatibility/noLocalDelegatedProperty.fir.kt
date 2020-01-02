// !LANGUAGE: -LocalDelegatedProperties
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

fun foo(): Int {
    val prop: Int by Delegate()

    val prop2: Int by 123

    val obj = object {
        fun v(): Int {
            val prop3: Int by Delegate()
            return prop3
        }
    }

    return prop + prop2 + obj.v()
}

import kotlin.reflect.KProperty

class MyProperty {
    fun getValue(thisRef: Any?, desc: KProperty<*>) = ":)"
}

val Any.ext by MyProperty()

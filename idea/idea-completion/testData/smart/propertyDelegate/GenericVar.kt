import kotlin.reflect.KProperty

class X {
    operator fun getValue(thisRef: List<String>, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: List<String>, property: KProperty<*>, value: String){}
}

var <T> List<T>.property: T by <caret>

// EXIST: X
import kotlin.reflect.KProperty

class X {
    operator fun getValue(thisRef: List<String>, property: KProperty<*>): String = ""
}

val <T> List<T>.property: T by <caret>

// EXIST: lazy
// EXIST: X
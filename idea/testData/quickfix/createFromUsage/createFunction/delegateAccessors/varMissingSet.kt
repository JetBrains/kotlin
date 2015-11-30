// "Create member function 'setValue'" "true"
import kotlin.reflect.KProperty

class F {
    operator fun getValue(x: X, property: KProperty<*>): Int = 1
}

class X {
    var f: Int by F()<caret>
}

// "Create member function 'getValue'" "true"
import kotlin.reflect.KProperty

class F {
    fun setValue(x: X, property: KProperty<*>, i: Int) { }
}

class X {
    var f: Int by F()<caret>
}

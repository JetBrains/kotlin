// "Change to val" "false"
// ACTION: Create extension function 'Delegate.getValue'
// ACTION: Create member function 'Delegate.getValue'
// ACTION: Introduce import alias
// ERROR: Type 'Delegate' has no method 'getValue(Nothing?, KProperty<*>)' and thus it cannot serve as a delegate
import kotlin.reflect.KProperty

fun test() {
    var foo: String by <caret>Delegate()
}

class Delegate {
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }
}

// "Change to val" "false"
// ACTION: Create extension function 'Delegate.getValue'
// ACTION: Create member function 'Delegate.getValue'
// ERROR: Missing 'getValue(Nothing?, KProperty<*>)' method on delegate of type 'Delegate'
import kotlin.reflect.KProperty

fun test() {
    var foo: String by <caret>Delegate()
}

class Delegate {
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }
}
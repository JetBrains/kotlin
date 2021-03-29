// "Change to val" "false"
// ACTION: Create extension function 'Delegate.getValue', function 'Delegate.setValue'
// ACTION: Create member function 'Delegate.getValue', function 'Delegate.setValue'
// ACTION: Introduce import alias
// ERROR: Type 'Delegate' has no method 'getValue(Nothing?, KProperty<*>)' and thus it cannot serve as a delegate
// ERROR: Type 'Delegate' has no method 'setValue(Nothing?, KProperty<*>, String)' and thus it cannot serve as a delegate for var (read-write property)
import kotlin.reflect.KProperty

fun test() {
    var foo: String by <caret>Delegate()
}

class Delegate

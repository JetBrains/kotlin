// "Change to val" "false"
// ACTION: Create extension function 'Delegate.getValue', function 'Delegate.setValue'
// ACTION: Create member function 'Delegate.getValue', function 'Delegate.setValue'
// ERROR: Missing 'getValue(Nothing?, KProperty<*>)' method on delegate of type 'Delegate'
// ERROR: Missing 'setValue(Nothing?, KProperty<*>, String)' method on delegate of type 'Delegate'
import kotlin.reflect.KProperty

fun test() {
    var foo: String by <caret>Delegate()
}

class Delegate
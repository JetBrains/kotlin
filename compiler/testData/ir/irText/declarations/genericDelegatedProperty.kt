class C<T>

object Delegate {
    operator fun getValue(thisRef: Any?, kProp: Any?) = 42
    operator fun setValue(thisRef: Any?, kProp: Any? , newValue: Int) {}
}

var <T> C<T>.genericDelegatedProperty by Delegate

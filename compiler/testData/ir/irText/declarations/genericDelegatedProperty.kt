// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// ^ KT-57818

class C<T>

object Delegate {
    operator fun getValue(thisRef: Any?, kProp: Any?) = 42
    operator fun setValue(thisRef: Any?, kProp: Any? , newValue: Int) {}
}

var <T> C<T>.genericDelegatedProperty by Delegate

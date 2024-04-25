// FIR_IDENTICAL
// LANGUAGE: +ForbidUsingExtensionPropertyTypeParameterInDelegate

class Delegate<T : Any> {
    private var v: T? = null
    operator fun getValue(thisRef: Any?, kp: Any?): T = v!!
    operator fun setValue(thisRef: Any?, kp: Any?, newValue: T) { v = newValue }
}

var <T : Any> List<T>.foo <!DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER_ERROR!>by Delegate<T>()<!>

class Wrapper<T>(val v: T? = null)

operator fun <T> Wrapper<T>.getValue(thisRef: Any?, kp: Any?): T = v!!

val <T : Any> List<T>.bar <!DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER_ERROR!>by Wrapper<T>()<!>

fun useString(s: String) {}

fun main(listInt: List<Int>, listStr: List<String>) {
    listInt.foo = 42
    useString(listStr.foo) // CCE
}

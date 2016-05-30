// !LANGUAGE: -LocalDelegatedProperties
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

fun foo(): Int {
    val prop: Int <!LOCAL_VARIABLE_WITH_DELEGATE!>by Delegate()<!>

    val prop2: Int <!LOCAL_VARIABLE_WITH_DELEGATE!>by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>123<!><!>

    return prop + prop2
}

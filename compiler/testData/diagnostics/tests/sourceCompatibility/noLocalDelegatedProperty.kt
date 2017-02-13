// !LANGUAGE: -LocalDelegatedProperties
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

fun foo(): Int {
    val prop: Int <!UNSUPPORTED_FEATURE!>by Delegate()<!>

    val prop2: Int <!UNSUPPORTED_FEATURE!>by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>123<!><!>

    val obj = object {
        fun v(): Int {
            val prop3: Int <!UNSUPPORTED_FEATURE!>by Delegate()<!>
            return prop3
        }
    }

    return prop + prop2 + obj.v()
}

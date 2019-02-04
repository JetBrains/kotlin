// !LANGUAGE: -LocalDelegatedProperties

// FILE: script.kts
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

fun foo(): Int {
    val prop: Int <!UNSUPPORTED_FEATURE!>by Delegate()<!>

    val prop2: Int <!UNSUPPORTED_FEATURE!>by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>123<!><!>

    return prop + prop2
}


val prop: Int by Delegate()

val prop2: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>123<!>

prop + prop2
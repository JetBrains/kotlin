// LANGUAGE: -DirectFieldOrDelegateAccess

import kotlin.reflect.KProperty

var number by internal object {
    var rawValue = 10

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return rawValue.toString()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        rawValue = value.length
    }
}

fun updateNumber() {
    <!UNSUPPORTED_FEATURE!>number#rawValue = 20<!>
    val rawValue: Int = <!UNSUPPORTED_FEATURE!>number#rawValue<!>
}

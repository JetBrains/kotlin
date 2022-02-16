// MODULE: a
// FILE: A.kt

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

// MODULE: b(a)
// FILE: B.kt

fun box(): String {
    if (number != "10") {
        return "FAIL: expected \"10\", was $number"
    }

    number = "20"

    if (number != "2") {
        return "FAIL: expected \"2\", was $number"
    }

    number#<!INVISIBLE_REFERENCE!>rawValue<!> = 20

    if (number != "20") {
        return "FAIL: expected \"20\", was $number"
    }

    number = "100"
    val rawValue: Int = number#<!INVISIBLE_REFERENCE!>rawValue<!>

    if (rawValue != 3) {
        return "FAIL: expected \"3\", was $rawValue"
    }

    return "OK"
}

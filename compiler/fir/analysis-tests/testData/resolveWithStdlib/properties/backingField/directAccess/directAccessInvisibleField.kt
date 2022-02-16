// MODULE: a
// FILE: A.kt

var number: String
    internal field = 10
    get() = field.toString()
    set(newValue) {
        field = newValue.length
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

    number#<!INVISIBLE_REFERENCE!>field<!> = 20

    if (number != "20") {
        return "FAIL: expected \"20\", was $number"
    }

    number = "100"
    val rawValue: Int = number#<!INVISIBLE_REFERENCE!>field<!>

    if (rawValue != 3) {
        return "FAIL: expected \"3\", was $rawValue"
    }

    return "OK"
}

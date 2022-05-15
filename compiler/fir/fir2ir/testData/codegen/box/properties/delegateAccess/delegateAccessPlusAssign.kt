import kotlin.reflect.KProperty

var number by object {
    var rawValue = 10

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return rawValue.toString()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        rawValue = value.length
    }
}

fun box(): String {
    number += "test"

    if (number != "6") {
        return "FAIL: expected 6, was $number"
    }

    number#rawValue += 4

    if (number != "10") {
        return "FAIL: expected 10, was $number"
    }

    return "OK"
}

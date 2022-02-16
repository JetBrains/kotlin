import kotlin.reflect.KProperty

class A {
    var number by object {
        var rawValue = 10

        operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return rawValue.toString()
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            rawValue = value.length
        }
    }
}

fun box(): String {
    val a = A()

    if (a.number != "10") {
        return "FAIL: expected \"10\", was ${a.number}"
    }

    a.number = "20"

    if (a.number != "2") {
        return "FAIL: expected \"2\", was ${a.number}"
    }

    a.number#rawValue = 20

    if (a.number != "20") {
        return "FAIL: expected \"20\", was ${a.number}"
    }

    a.number = "100"
    val rawValue: Int = a.number#rawValue

    if (rawValue != 3) {
        return "FAIL: expected \"3\", was $rawValue"
    }

    return "OK"
}

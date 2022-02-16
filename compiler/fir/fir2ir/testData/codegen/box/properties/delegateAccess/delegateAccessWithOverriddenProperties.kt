import kotlin.reflect.KProperty

open class A {
    open var rawValue = 40
        get() = field * 0
        set(value) {}
}

var number by object : A() {
    class ValueHolder(var value: Int)

    override var rawValue: Int
        internal field = ValueHolder(10)
        get() = field.value
        set(value) {
            field.value = value
        }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return rawValue.toString()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        rawValue = value.length
    }
}

fun box(): String {
    if (number != "10") {
        return "FAIL: expected \"10\", was $number"
    }

    number = "20"

    if (number != "2") {
        return "FAIL: expected \"2\", was $number"
    }

    number#rawValue = 20

    if (number != "20") {
        return "FAIL: expected \"20\", was $number"
    }

    number = "100"
    val rawValue: Int = number#rawValue

    if (rawValue != 3) {
        return "FAIL: expected \"3\", was $rawValue"
    }

    number#rawValue#field.value = 400

    if (number != "400") {
        return "FAIL: expected \"400\", was $number"
    }

    return "OK"
}

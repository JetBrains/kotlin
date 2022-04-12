import kotlin.reflect.KProperty

open class A

val A.text get() = "Test: "

var number by object : A() {
    var rawValue = 10

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return text + rawValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        rawValue = value.length
    }
}

fun box(): String {
    if (number != "Test: 10") {
        return "FAIL: expected \"Test: 10\", was $number"
    }

    if (number#text != "Test: ") {
        return "FAIL: expected \"Test: \", was ${number#text}"
    }

    return "OK"
}

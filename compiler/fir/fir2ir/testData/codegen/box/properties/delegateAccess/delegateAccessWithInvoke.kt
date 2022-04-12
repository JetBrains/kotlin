import kotlin.reflect.KProperty

val myProperty by object {
    var action = object {
        operator fun invoke(number: Int) = number * 2
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = "Hello"
}

fun box(): String {
    if (myProperty#action(20) != 40) {
        return "FAIL: expected 40"
    }

    return "OK"
}

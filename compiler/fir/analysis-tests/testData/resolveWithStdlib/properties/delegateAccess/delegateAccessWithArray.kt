import kotlin.reflect.KProperty

val number by object {
    val rawValue = mutableListOf(10, 20)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return rawValue.joinToString(", ")
    }
}

fun box(): String {
    number#rawValue[1] += 4

    if (number != "10, 24") {
        return "FAIL: expected `10, 24`, was $number"
    }

    return "OK"
}

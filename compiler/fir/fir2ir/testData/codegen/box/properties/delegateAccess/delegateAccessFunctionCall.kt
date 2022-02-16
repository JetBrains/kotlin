import kotlin.reflect.KProperty

val myProperty by object {
    fun represent(decoration: String) = "$decoration Some representation $decoration"
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = "Hello"
}

fun box(): String {
    if (myProperty#represent("~~~") != "~~~ Some representation ~~~") {
        return "FAIL: expected \"~~~ Some representation ~~~\""
    }

    return "OK"
}

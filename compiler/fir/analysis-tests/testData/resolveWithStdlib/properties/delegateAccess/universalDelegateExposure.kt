import kotlin.reflect.KProperty

val <T> T.delegate get() = this

val myProperty by object {
    var text = "Test"
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = text
}

fun box(): String {
    myProperty#delegate.apply {
        text = "Rest"
    }

    if (myProperty != "Rest") {
        return "FAIL: expected \"Rest\", was $myProperty"
    }

    return "OK"
}

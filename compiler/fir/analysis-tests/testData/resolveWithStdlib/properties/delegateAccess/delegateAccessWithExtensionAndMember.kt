import kotlin.reflect.KProperty

class MyDelegate {
    var text = "Test"
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = text
}

val myProperty by MyDelegate()

val <T> T.text get() = 20

fun test() {
    val a: String = myProperty#text
    val b: Int = <!INITIALIZER_TYPE_MISMATCH!>myProperty#text<!>
}

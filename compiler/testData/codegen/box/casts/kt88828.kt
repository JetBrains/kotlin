open class Base<T> {
    open var x: T? = null
}

class Derived : Base<String>()

class Data(val x: Int)

fun garble(derived: Derived) {
    (derived as Base<Data>).x = Data(42)
}

fun box(): String {
    val derived = Derived()
    garble(derived)

    return try {
        val value = derived.x
        "FAIL: ${value?.length}"
    } catch (e: ClassCastException) {
        "OK"
    }
}

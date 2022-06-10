import kotlin.reflect.KProperty

class A {
    var number by internal object {
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
    val previousNumber: Int = a.number#self#self#rawValue#self.dec()

    if (previousNumber != 9) {
        return "FAIL: expected \"9\", was ${previousNumber}"
    }

    return "OK"
}

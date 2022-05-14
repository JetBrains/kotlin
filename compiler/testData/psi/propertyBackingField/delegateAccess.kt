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

    fun updateNumber() {
        number#rawValue += 100
    }

    fun represent(): String {
        return "rawValue = " + number#rawValue
    }
}

fun previousNumber(a: A): Int {
    val value: Int = a.number#self#self#rawValue#self.dec()
    return value
}

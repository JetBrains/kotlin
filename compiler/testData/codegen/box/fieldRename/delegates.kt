// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.reflect.KProperty

public open class TestDelegate<T: Any>(private val initializer: () -> T) {
    private var value: T? = null

    operator open fun getValue(thisRef: Any?, desc: KProperty<*>): T {
        if (value == null) {
            value = initializer()
        }
        return value!!
    }

    operator open fun setValue(thisRef: Any?, desc: KProperty<*>, svalue : T) {
        value = svalue
    }
}

class A
class B
class C
class D

public val A.s: String by TestDelegate({"A"})
public val B.s: String by TestDelegate({"B"})
public val C.s: String by TestDelegate({"C"})
public val D.s: String by TestDelegate({"D"})

fun box() : String {
    if (A().s != "A") return "Fail A"
    if (B().s != "B") return "Fail B"
    if (C().s != "C") return "Fail C"
    if (D().s != "D") return "Fail D"

    return "OK"
}

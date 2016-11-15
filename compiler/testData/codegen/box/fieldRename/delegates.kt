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

class A {}
class B {}

public val A.s: String by TestDelegate( {"OK2"})
public val B.s: String by TestDelegate( {"OK"})

fun box() : String {
  if (A().s != "OK2") return "fail1"

  return B().s
}

public open class TestDelegate<T: Any>(private val initializer: () -> T) {
    private volatile var value: T? = null

    public open fun get(thisRef: Any?, desc: PropertyMetadata): T {
        if (value == null) {
            value = initializer()
        }
        return value!!
    }

    public open fun set(thisRef: Any?, desc: PropertyMetadata, svalue : T) {
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

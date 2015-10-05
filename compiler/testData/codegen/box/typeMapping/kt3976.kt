// java.lang.ClassNotFoundException: kotlin.Nothing

var currentAccountId: Int? by SessionAccessor()
class SessionAccessor<T> {
    fun getValue(o : Nothing?, desc: PropertyMetadata): T {
        return null as T
    }

    fun setValue(o : Nothing?, desc: PropertyMetadata, value: T) {

    }
}

fun box(): String {
    currentAccountId = 1
    if (currentAccountId != null) return "Fail"
    return "OK"
}

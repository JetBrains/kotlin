// java.lang.ClassNotFoundException: jet.Nothing

var currentAccountId: Int? by SessionAccessor()
class SessionAccessor<T> {
    fun get(o : Nothing?, desc: jet.PropertyMetadata): T {
        return null as T
    }

    fun set(o : Nothing?, desc: jet.PropertyMetadata, value: T) {

    }
}

fun box(): String {
    currentAccountId = 1
    if (currentAccountId != null) return "Fail"
    return "OK"
}

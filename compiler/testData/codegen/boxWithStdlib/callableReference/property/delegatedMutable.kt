var result: String by Delegate

object Delegate {
    var value = "lol"

    fun getValue(instance: Any?, data: PropertyMetadata): String {
        return value
    }

    fun setValue(instance: Any?, data: PropertyMetadata, newValue: String) {
        value = newValue
    }
}

fun box(): String {
    val f = ::result
    if (f.get() != "lol") return "Fail 1: {$f.get()}"
    Delegate.value = "rofl"
    if (f.get() != "rofl") return "Fail 2: {$f.get()}"
    f.set("OK")
    return f.get()
}

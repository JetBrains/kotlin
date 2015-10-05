class Delegate {
    var inner = "OK"
    fun getValue(t: Any?, p: PropertyMetadata): String = inner

    private fun propertyDelegated(p: PropertyMetadata) { inner = "fail" }
    fun propertyDelegated() { inner = "fail" }
    fun propertyDelegated(a: Int) { inner = "fail" }
    fun propertyDelegated(a: String) { inner = "fail" }
    fun propertyDelegated(p: PropertyMetadata, a: Int) { inner = "fail" }
    fun <T> propertyDelegated(p: PropertyMetadata, s: String = "") { inner = "fail" }
}

val prop by Delegate()

fun box(): String {
    return prop
}

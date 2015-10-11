class Delegate {
    var name = ""
    fun getValue(t: Any?, p: PropertyMetadata): String = name
    fun propertyDelegated(p: PropertyMetadata) { name = p.name }
}

class A {
    val p = Delegate()
    val prop by p
}

fun box(): String {
    return if (A().prop == "prop") "OK" else "fail"
}

class Delegate {
    var name = ""
    fun getValue(t: Any?, p: PropertyMetadata): String = name
    fun propertyDelegated(p: PropertyMetadata) { name = p.name }
}

val p = Delegate()

class A {
    val prop by p
}

fun box(): String {
    return if (A().prop == "prop") "OK" else "fail"
}

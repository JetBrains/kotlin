class Delegate {
    var name = ""
    fun getValue(t: Any?, p: PropertyMetadata): String = name
}

fun Delegate.propertyDelegated(p: PropertyMetadata) { name = p.name }

class A {
    val prop by Delegate()
}

fun box(): String {
    return if (A().prop == "prop") "OK" else "fail"
}

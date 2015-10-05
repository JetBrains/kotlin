class Delegate {
    var name = ""
    fun getValue(t: Any?, p: PropertyMetadata): String = name
    fun propertyDelegated(p: PropertyMetadata) { name = p.name }
}

fun foo() = Delegate()

class A {
    val prop by foo()
}

fun box(): String {
    return if (A().prop == "prop") "OK" else "fail"
}

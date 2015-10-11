class Delegate {
    var name = ""
    fun getValue(t: A, p: PropertyMetadata): String = name
    fun propertyDelegated(p: PropertyMetadata) { name = p.name }
}

val A.prop by Delegate()

class A {
}

fun box(): String {
    return if (A().prop == "prop") "OK" else "fail"
}

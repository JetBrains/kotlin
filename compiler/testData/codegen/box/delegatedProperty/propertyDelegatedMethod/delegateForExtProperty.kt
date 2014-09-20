class Delegate {
    var name = ""
    fun get(t: A, p: PropertyMetadata): String = name
    fun propertyDelegated(p: PropertyMetadata) { name = p.name }
}

val A.prop by Delegate()

class A {
}

fun box(): String {
    return if (A().prop == "prop") "OK" else "fail"
}

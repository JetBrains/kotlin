class Delegate {
    var name = ""
    fun getValue(t: F.A, p: PropertyMetadata): String = name
    fun propertyDelegated(p: PropertyMetadata) { name = p.name }
}

class F {
    val A.prop by Delegate()

    class A {
    }

    fun foo(): String {
        return A().prop
    }
}

fun box(): String {
    return if (F().foo() == "prop") "OK" else "fail"
}

class Delegate {
    var name = ""
    fun getValue(t: Any?, p: PropertyMetadata): String = name
    fun propertyDelegated(p: PropertyMetadata) { name = p.name }
}

class A {
    inner class B {
        val prop by Delegate()
    }
}

fun box(): String {
    val p = A().B().prop
    return if(p == "prop") "OK" else "fail"
}

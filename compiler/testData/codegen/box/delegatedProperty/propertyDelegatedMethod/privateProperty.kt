class Delegate {
    var name = ""
    fun getValue(t: Any?, p: PropertyMetadata): String = name
    fun propertyDelegated(p: PropertyMetadata) { name = p.name }
}

class A {
    private val prop by Delegate()

    fun test(): String {
        return if (prop == "prop") "OK" else "fail"
    }
}

fun box(): String {
    return A().test()
}

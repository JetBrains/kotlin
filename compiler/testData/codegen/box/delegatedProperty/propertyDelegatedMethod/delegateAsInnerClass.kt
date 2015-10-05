class A {
    val prop: String by Delegate()

    inner class Delegate {
        var name = ""
        fun getValue(t: Any?, p: PropertyMetadata): String = name
        fun propertyDelegated(p: PropertyMetadata) { name = p.name }
    }
}

fun box(): String {
    return if (A().prop == "prop") "OK" else "fail"
}

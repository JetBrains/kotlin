class Delegate {
    var name = ""
    fun getValue(t: Any?, p: PropertyMetadata): String = name
    fun propertyDelegated(vararg p: PropertyMetadata) { name = p[0].name }
}

val prop by Delegate()

fun box(): String {
    return if (prop == "prop") "OK" else "fail"
}

class Delegate {
    var name = ""
    fun getValue(t: Any?, p: PropertyMetadata): String = name
    fun propertyDelegated(p: PropertyMetadata, s: String = "is OK") { name = "${p.name} $s" }
}

val prop by Delegate()

fun box(): String {
    return if (prop == "prop is OK") "OK" else "fail";
}

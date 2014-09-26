class Delegate {
    var name = ""
    fun get(t: Any?, p: PropertyMetadata): String = name
    fun propertyDelegated(p: PropertyMetadata) { name = p.name }
}

trait A {
    val prop: String
}

class AImpl: A  {
    override val prop by Delegate()
}

fun box(): String {
    return if(AImpl().prop == "prop") "OK" else "fail"
}

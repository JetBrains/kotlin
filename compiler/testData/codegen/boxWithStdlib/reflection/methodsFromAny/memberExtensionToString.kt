import kotlin.reflect.jvm.kotlin

class A {
    var String.id: String
        get() = this
        set(value) {}
}

fun box(): String {
    val p = javaClass<A>().kotlin.getExtensionProperties().single()
    return if ("$p" == "var A.(kotlin.String.)id") "OK" else "Fail $p"
}

import kotlin.reflect.*

class A {
    var String.id: String
        get() = this
        set(value) {}

    fun Int.foo(): Double = toDouble()
}

fun box(): String {
    val p = javaClass<A>().kotlin.memberExtensionProperties.single()
    return if ("$p" == "var A.(kotlin.String.)id: kotlin.String") "OK" else "Fail $p"

    val q = javaClass<A>().kotlin.declaredFunctions.single()
    if ("$q" != "fun A.(kotlin.Int.)foo(): kotlin.Double") return "Fail q $q"

    return "OK"
}

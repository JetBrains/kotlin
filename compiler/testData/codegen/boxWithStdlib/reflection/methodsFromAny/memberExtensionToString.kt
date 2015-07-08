import kotlin.reflect.jvm.kotlin
import kotlin.reflect.*

class A {
    var String.id: String
        get() = this
        set(value) {}
}

fun box(): String {
    val p = javaClass<A>().kotlin.extensionProperties.single()
    return if ("$p" == "var A.(kotlin.String.)id") "OK" else "Fail $p"
}

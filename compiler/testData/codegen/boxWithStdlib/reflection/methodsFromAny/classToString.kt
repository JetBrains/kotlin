import kotlin.test.*
import kotlin.reflect.jvm.kotlin

class A

fun box(): String {
    val p = javaClass<A>().kotlin
    if ("$p" != "class A") return "Fail: $p"

    val s = javaClass<String>().kotlin
    if ("$s" != "class java.lang.String") return "Fail: $s"

    return "OK"
}

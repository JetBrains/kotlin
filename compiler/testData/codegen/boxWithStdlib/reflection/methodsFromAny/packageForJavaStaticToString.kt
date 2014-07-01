import kotlin.test.*
import kotlin.reflect.jvm.kotlinPackage

fun box(): String {
    val p = javaClass<String>().kotlinPackage
    if ("$p" != "package java.lang.String") return "Fail: $p"
    return "OK"
}

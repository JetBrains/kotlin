import kotlin.test.*
import kotlin.reflect.jvm.kotlinPackage

fun box(): String {
    val p = Class.forName("_DefaultPackage").kotlinPackage
    if ("$p" != "package <default>") return "Fail: $p"
    return "OK"
}

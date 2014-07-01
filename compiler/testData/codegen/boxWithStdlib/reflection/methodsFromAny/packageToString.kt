package test.foo.bar

import kotlin.test.*
import kotlin.reflect.jvm.kotlinPackage

fun box(): String {
    val p = Class.forName("test.foo.bar.BarPackage").kotlinPackage
    if ("$p" != "package test.foo.bar") return "Fail: $p"
    return "OK"
}

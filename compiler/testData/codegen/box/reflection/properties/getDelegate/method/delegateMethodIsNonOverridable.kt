// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.isAccessible

val a = 1
val b = 2

fun KProperty0<*>.test(): String =
    (apply { isAccessible = true }.getDelegate() as KProperty<*>).name

open class C {
    open val x by run { ::a }
    open val y by ::a

    val xc = ::x.test()
    val yc = ::y.test()
}

class D : C() {
    override val x by run { ::b }
    override val y by ::b

    val xd = ::x.test()
    val yd = ::y.test()
}

fun box(): String {
    val result = D().run { "$xc $yc $xd $yd" }
    if (result != "a a b b") return "Fail: $result"

    return "OK"
}

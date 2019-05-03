// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KProperty

object Delegate {
    operator fun getValue(thiz: Any?, property: KProperty<*>): String {
        return property.name + ":" + property.returnType
    }
}

class C {
    val a by Delegate

    fun test(): String {
        if (a != "a:kotlin.String") return "Fail a: $a"

        val b by Delegate
        if (b != "b:kotlin.String") return "Fail b: $b"

        return "OK"
    }
}

val x by Delegate

fun box(): String {
    if (x != "x:kotlin.String") return "Fail x: $x"

    val y by Delegate
    if (y != "y:kotlin.String") return "Fail y: $y"

    return C().test()
}

// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.KCallable
import kotlin.test.assertEquals

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Ann(val who: String)

object O {
    fun f(@Ann("on-x") x: Int, @Ann("on-y") y: String) {}

    @JvmStatic
    fun g(@Ann("on-x") x: Int, @Ann("on-y") y: String) {}
}

class C {
    companion object {
        fun v(@Ann("on-x") x: Int, @Ann("on-y") y: String) {}

        @JvmStatic
        fun w(@Ann("on-x") x: Int, @Ann("on-y") y: String) {}
    }
}

private fun check(expected: String, unbound: KCallable<*>, bound: KCallable<*>) {
    assertEquals(expected, unbound.parameters.map { it.annotations.map { (it as Ann).who } }.joinToString())
    assertEquals(expected.substringAfter(", "), bound.parameters.map { it.annotations.map { (it as Ann).who } }.joinToString())
}

fun box(): String {
    check("[], [on-x], [on-y]", O::class.members.single { it.name == "f" }, O::f)
    check("[], [on-x], [on-y]", O::class.members.single { it.name == "g" }, O::g)
    check("[], [on-x], [on-y]", C.Companion::class.members.single { it.name == "v" }, C.Companion::v)
    check("[], [on-x], [on-y]", C.Companion::class.members.single { it.name == "w" }, C.Companion::w)
    return "OK"
}

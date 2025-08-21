// LANGUAGE: +ContextParameters
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.KParameter.Kind.*
import kotlin.test.assertEquals

class Z {
    context(_: String, named: Int, _: Number)
    fun f() {}
}

fun box(): String {
    val f = Z::class.members.single { it.name == "f" }
    assertEquals(listOf(null, null, "named", null), f.parameters.map { it.name })
    return "OK"
}

// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

open class A<T> {
    fun f(x: T): CharSequence = "T"
    fun f(x: Any): String = "Any"
}

class B : A<String>()

fun box(): String {
    assertEquals(listOf("T", "kotlin.Any"), A::class.members.filter { it.name == "f" }.map { it.parameters[1].type.toString() }.sorted())
    assertEquals(listOf("kotlin.Any", "kotlin.String"), B::class.members.filter { it.name == "f" }.map { it.parameters[1].type.toString() }.sorted())
    return "OK"
}

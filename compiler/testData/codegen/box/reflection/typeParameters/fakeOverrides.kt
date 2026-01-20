// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.reflect.*

open class A {
    fun <T> f(t: T): T = t
}

class B : A()

fun box(): String {
    val f = B::class.members.single { it.name == "f" }

    assertEquals(f.typeParameters.first(), f.returnType.classifier)
    assertEquals(f.typeParameters.first(), f.parameters.last().type.classifier)

    return "OK"
}

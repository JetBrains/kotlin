// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class A<T>
class B<T>

class Fun {
    fun <T> foo(): T = null!!
}

class Fourple<A, B, in C, out D>

fun box(): String {
    assertEquals(A::class.typeParameters, A::class.typeParameters)
    assertEquals(A::class.typeParameters.single().hashCode(), A::class.typeParameters.single().hashCode())

    fun getFoo() = Fun::class.members.single { it.name == "foo" }
    assertEquals(getFoo().typeParameters, getFoo().typeParameters)
    assertEquals(getFoo().typeParameters.single().hashCode(), getFoo().typeParameters.single().hashCode())

    assertNotEquals(A::class.typeParameters.single(), B::class.typeParameters.single())

    val fi = Fourple::class.typeParameters
    val fj = Fourple::class.typeParameters
    for (i in 0..fi.size - 1) {
        for (j in 0..fj.size - 1) {
            if (i == j) {
                assertEquals(fi[i], fj[j])
                assertEquals(fi[i].hashCode(), fj[j].hashCode())
            } else {
                assertNotEquals(fi[i], fj[j])
            }
        }
    }

    return "OK"
}

import kotlin.reflect.*
import kotlin.test.assertEquals

class A {
    val x = 1
    fun x(): String = "OK"
}

val f1: KProperty1<A, Int> = A::x
val f2: (A) -> String = A::x

fun box(): String {
    val a = A()

    assertEquals(1, f1.get(a))
    assertEquals("OK", f2(a))

    return "OK"
}
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM
// DONT_TARGET_EXACT_BACKEND: JS

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

package test

import kotlin.reflect.KClass
import kotlin.test.assertTrue as assert
import kotlin.test.assertEquals

enum class E { A, B }

annotation class A()

annotation class B(val a: A = A())

annotation class C(
    val i: Int = 42,
    val b: B = B(),
    val kClass: KClass<*> = B::class,
    val kClassArray: Array<KClass<*>> = [E::class, A::class],
    val e: E = E.B,
    val aS: Array<String> = arrayOf("a", "b"),
    val aI: IntArray = intArrayOf(1, 2)
)

annotation class EmptyDefaultArrays(
    val kClassArray: Array<KClass<*>> = [],
    val intArray: IntArray = [],
    val stringArray: Array<String> = [],
    val enumArray : Array<E> = []
)

annotation class Partial(
    val i: Int = 42,
    val s: String = "foo",
    val e: E = E.A
)

fun box(): String {
    val c = C()
    assertEquals(42, c.i)
    assertEquals(A(), c.b.a)
    assertEquals(B::class, c.kClass)
    assertEquals(2, c.kClassArray.size)
    assertEquals(E.B, c.e)
    assert(arrayOf("a", "b").contentEquals(c.aS))
    assert(intArrayOf(1, 2).contentEquals(c.aI))
    val p = Partial(e = E.B, s = "bar")
    assertEquals(42, p.i)
    assertEquals("bar", p.s)
    assertEquals(E.B, p.e)
    val eda = EmptyDefaultArrays()
    assertEquals(0, eda.intArray.size)
    assertEquals(0, eda.kClassArray.size)
    assertEquals(0, eda.stringArray.size)
    assertEquals(0, eda.enumArray.size)
    return "OK"
}

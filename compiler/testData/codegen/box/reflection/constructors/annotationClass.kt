// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertEquals

annotation class A1

annotation class A2(val k: KClass<*>, val s: A1)

fun box(): String {
    assertEquals(1, A1::class.constructors.size)
    assertEquals(A1::class.primaryConstructor, A1::class.constructors.single())

    val cs = A2::class.constructors
    assertEquals(1, cs.size)
    assertEquals(A2::class.primaryConstructor, cs.single())
    val params = cs.single().parameters
    assertEquals(listOf("k", "s"), params.map { it.name })

    return "OK"
}

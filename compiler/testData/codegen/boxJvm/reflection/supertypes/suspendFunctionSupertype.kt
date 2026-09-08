// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.coroutines.*
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class S0 : suspend () -> Unit
abstract class S1 : suspend (String) -> String
abstract class S1N : suspend (Int) -> String?
abstract class S0S0 : suspend () -> suspend () -> Any

fun any(): Any = null!!
fun functionUnit(): Function<Unit> = null!!
fun functionString(): Function<String> = null!!
fun functionStringN(): Function<String?> = null!!
fun functionS0(): Function<suspend () -> Any> = null!!
fun s0(): suspend () -> Unit = null!!
fun s1(): suspend (String) -> String = null!!
fun s1n(): suspend (Int) -> String? = null!!
fun s0s0(): suspend () -> suspend() -> Any = null!!

fun KClass<*>.checkSupertypes(vararg expected: KCallable<*>) =
    assertEquals(expected.map { it.returnType }, supertypes)
fun KClass<*>.checkAllSupertypes(vararg expected: KCallable<*>) =
    assertEquals(expected.map { it.returnType }.toSet(), allSupertypes.toSet())
fun KClass<*>.checkSuperclasses(vararg expected: KClass<*>) =
    assertEquals(expected.toList(), superclasses)
fun KClass<*>.checkAllSuperclasses(vararg expected: KClass<*>) =
    assertEquals(expected.toSet(), allSuperclasses.toSet())

fun box(): String {
    with(S0::class) {
        checkSupertypes(::s0, ::any)
        checkAllSupertypes(::s0, ::functionUnit, ::any)
        checkSuperclasses(Function1::class, Any::class)
        checkAllSuperclasses(Function1::class, Function::class, Any::class)
    }
    with(S1::class) {
        checkSupertypes(::s1, ::any)
        checkAllSupertypes(::s1, ::functionString, ::any)
        checkSuperclasses(Function2::class, Any::class)
        checkAllSuperclasses(Function2::class, Function::class, Any::class)
    }
    with(S1N::class) {
        checkSupertypes(::s1n, ::any)
        checkAllSupertypes(::s1n, ::functionStringN, ::any)
        checkSuperclasses(Function2::class, Any::class)
        checkAllSuperclasses(Function2::class, Function::class, Any::class)
    }
    with(S0S0::class) {
        checkSupertypes(::s0s0, ::any)
        checkAllSupertypes(::s0s0, ::functionS0, ::any)
        checkSuperclasses(Function1::class, Any::class)
        checkAllSuperclasses(Function1::class, Function::class, Any::class)
    }

    assertEquals(4, S0::class.members.size)
    val invoke = S0::class.members.single { it.name == "invoke" } as KFunction<*>
    assertEquals("fun test.S0.invoke(): kotlin.Unit", invoke.toString())
    assertTrue(invoke.isSuspend)

    return "OK"
}

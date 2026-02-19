// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertEquals

abstract class S0 : () -> Unit
abstract class S1 : (String) -> String
abstract class S1N : (Int) -> String?
abstract class S0S0 : () -> () -> Any

fun any(): Any = null!!
fun functionUnit(): Function<Unit> = null!!
fun functionString(): Function<String> = null!!
fun functionStringN(): Function<String?> = null!!
fun functionS0(): Function<() -> Any> = null!!
fun s0(): () -> Unit = null!!
fun s1(): (String) -> String = null!!
fun s1n(): (Int) -> String? = null!!
fun s0s0(): () -> () -> Any = null!!

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
        checkSuperclasses(Function0::class, Any::class)
        checkAllSuperclasses(Function0::class, Function::class, Any::class)
    }
    with(S1::class) {
        checkSupertypes(::s1, ::any)
        checkAllSupertypes(::s1, ::functionString, ::any)
        checkSuperclasses(Function1::class, Any::class)
        checkAllSuperclasses(Function1::class, Function::class, Any::class)
    }
    with(S1N::class) {
        checkSupertypes(::s1n, ::any)
        checkAllSupertypes(::s1n, ::functionStringN, ::any)
        checkSuperclasses(Function1::class, Any::class)
        checkAllSuperclasses(Function1::class, Function::class, Any::class)
    }
    with(S0S0::class) {
        checkSupertypes(::s0s0, ::any)
        checkAllSupertypes(::s0s0, ::functionS0, ::any)
        checkSuperclasses(Function0::class, Any::class)
        checkAllSuperclasses(Function0::class, Function::class, Any::class)
    }

    return "OK"
}

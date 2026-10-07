// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.coroutines.*
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

abstract class S0 : suspend () -> Unit
abstract class S1 : suspend (String) -> String
abstract class S1N : suspend (Int) -> String?
abstract class S0S0 : suspend () -> suspend () -> Any

class S1Impl : S1() {
    override suspend fun invoke(p1: String): String = p1 + "K"
}

class Generic<T>(val first: T) : suspend (T) -> List<T> {
    override suspend fun invoke(p1: T): List<T> = listOf(first, p1)
}

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

private fun KClass<*>.invokeMember(): KFunction<*> = members.single { it.name == "invoke" } as KFunction<*>

private fun <T> runSuspend(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

private val useK1 = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true

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
    val invoke = S0::class.invokeMember()
    assertEquals("fun test.S0.invoke(): kotlin.Unit", invoke.toString())
    assertTrue(invoke.isSuspend)

    assertTrue(S1::class.invokeMember().isAbstract)
    val concrete = S1Impl::class.invokeMember()
    assertFalse(concrete.isAbstract)
    assertEquals(listOf(KParameter.Kind.INSTANCE, KParameter.Kind.VALUE), concrete.parameters.map { it.kind })
    assertEquals("OK", runSuspend { concrete.callSuspend(S1Impl(), "O") })
    // Calling the abstract member on a subclass instance dispatches to the override.
    if (useK1) {
        // K1-based implementation cannot compute a caller for the `invoke` of a suspend function type (`FunctionInvokeDescriptor`).
        assertFails { runSuspend { S1::class.invokeMember().callSuspend(S1Impl(), "O") } }
    } else {
        assertEquals("OK", runSuspend { S1::class.invokeMember().callSuspend(S1Impl(), "O") })
    }

    // Suspend function type parameterized by the implementing class's type parameter.
    assertEquals("[suspend (T) -> kotlin.collections.List<T>, kotlin.Any]", Generic::class.supertypes.toString())
    val generic = Generic::class.invokeMember()
    assertEquals("fun test.Generic<T>.invoke(T): kotlin.collections.List<T>", generic.toString())
    assertEquals(generic.returnType.arguments.single().type!!.classifier, Generic::class.typeParameters.single())
    assertEquals(listOf("a", "b"), runSuspend { generic.callSuspendBy(mapOf(generic.parameters[0] to Generic("a"), generic.parameters[1] to "b")) })

    return "OK"
}

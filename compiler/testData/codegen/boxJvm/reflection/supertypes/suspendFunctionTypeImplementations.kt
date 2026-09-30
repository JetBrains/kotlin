// TARGET_BACKEND: JVM
// WITH_REFLECT

// Complements suspendFunctionSupertype.kt (KT-87367, KT-87709, KT-81206) with suspend function types of higher arity
// or parameterized by the implementing class's type parameter, and checks that `invoke` obtained from `members` is callable.

package test

import kotlin.coroutines.*
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

class TwoParameters : suspend (String, Int) -> String {
    override suspend fun invoke(p1: String, p2: Int): String = p1.repeat(p2)
}

class Generic<T>(val first: T) : suspend (T) -> List<T> {
    override suspend fun invoke(p1: T): List<T> = listOf(first, p1)
}

abstract class AbstractInvoke : suspend () -> Int

class ConcreteInvoke : AbstractInvoke() {
    override suspend fun invoke(): Int = 42
}

private fun <T> runSuspend(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

private fun KClass<*>.invoke(): KFunction<*> = members.single { it.name == "invoke" } as KFunction<*>

private val useK1 = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true

fun box(): String {
    assertEquals("[suspend (kotlin.String, kotlin.Int) -> kotlin.String, kotlin.Any]", TwoParameters::class.supertypes.toString())
    assertEquals(listOf(Function3::class, Any::class), TwoParameters::class.superclasses)
    val twoParameters = TwoParameters::class.invoke()
    assertEquals("fun test.TwoParameters.invoke(kotlin.String, kotlin.Int): kotlin.String", twoParameters.toString())
    assertTrue(twoParameters.isSuspend)
    assertEquals(listOf(KParameter.Kind.INSTANCE, KParameter.Kind.VALUE, KParameter.Kind.VALUE), twoParameters.parameters.map { it.kind })
    assertEquals("ababab", runSuspend { twoParameters.callSuspend(TwoParameters(), "ab", 3) })

    assertEquals("[suspend (T) -> kotlin.collections.List<T>, kotlin.Any]", Generic::class.supertypes.toString())
    val generic = Generic::class.invoke()
    assertEquals("fun test.Generic<T>.invoke(T): kotlin.collections.List<T>", generic.toString())
    assertEquals(generic.returnType.arguments.single().type!!.classifier, Generic::class.typeParameters.single())
    assertEquals(listOf("a", "b"), runSuspend { generic.callSuspendBy(mapOf(generic.parameters[0] to Generic("a"), generic.parameters[1] to "b")) })

    assertTrue(AbstractInvoke::class.invoke().isAbstract)
    val concrete = ConcreteInvoke::class.invoke()
    assertFalse(concrete.isAbstract)
    assertEquals(42, runSuspend { concrete.callSuspend(ConcreteInvoke()) })
    // Calling the abstract member on a subclass instance dispatches to the override.
    if (useK1) {
        // K1-based implementation cannot compute a caller for the `invoke` of a suspend function type (`FunctionInvokeDescriptor`).
        assertFails { runSuspend { AbstractInvoke::class.invoke().callSuspend(ConcreteInvoke()) } }
    } else {
        assertEquals(42, runSuspend { AbstractInvoke::class.invoke().callSuspend(ConcreteInvoke()) })
    }

    return "OK"
}

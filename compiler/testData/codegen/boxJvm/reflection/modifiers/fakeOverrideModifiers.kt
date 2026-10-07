// TARGET_BACKEND: JVM
// WITH_REFLECT

// Fake overrides of Kotlin functions and properties keep the modifiers of the inherited declaration.
// See javaFakeOverrideModifiers.kt for Java methods and callableModifiersInMixedHierarchies.kt for mixed hierarchies.

import kotlin.coroutines.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.callSuspend
import kotlin.test.assertEquals
import kotlin.test.assertTrue

open class Base {
    operator fun plus(x: Int) = "Base.plus$x"
    infix fun infix(x: Int) = "Base.infix$x"
    inline fun inline() = "Base.inline"
    suspend fun suspend() = "Base.suspend"
    lateinit var lateinit: String
}

class FakeOverrides : Base()

private fun KClass<*>.function(name: String): KFunction<*> = members.single { it.name == name } as KFunction<*>

private fun <T> runSuspend(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

fun box(): String {
    val instance = FakeOverrides()

    FakeOverrides::class.function("plus").let {
        assertTrue(it.isOperator)
        assertEquals("Base.plus1", it.call(instance, 1))
    }
    FakeOverrides::class.function("infix").let {
        assertTrue(it.isInfix)
        assertEquals("Base.infix2", it.call(instance, 2))
    }
    FakeOverrides::class.function("inline").let {
        assertTrue(it.isInline)
        assertEquals("Base.inline", it.call(instance))
    }
    FakeOverrides::class.function("suspend").let {
        assertTrue(it.isSuspend)
        assertEquals("Base.suspend", runSuspend { it.callSuspend(instance) })
    }

    @Suppress("UNCHECKED_CAST")
    (FakeOverrides::class.members.single { it.name == "lateinit" } as KMutableProperty1<FakeOverrides, String>).let {
        assertTrue(it.isLateinit)
        it.set(instance, "value")
        assertEquals("value", it.get(instance))
    }

    return "OK"
}

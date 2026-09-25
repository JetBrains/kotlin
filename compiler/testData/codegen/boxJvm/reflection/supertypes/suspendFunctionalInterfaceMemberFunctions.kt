// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that classes implementing suspend functional interfaces expose their inherited
// member functions (invoke, equals, hashCode, toString) via memberFunctions.

import kotlin.reflect.full.*
import kotlin.test.*

class SuspendUnit : suspend () -> Unit {
    override suspend fun invoke() {}
}

class SuspendStringProducer : suspend () -> String {
    override suspend fun invoke(): String = "hello"
}

class SuspendMapper : suspend (Int) -> String {
    override suspend fun invoke(p: Int): String = p.toString()
}

fun checkMemberFunctions(klass: kotlin.reflect.KClass<*>, label: String) {
    val fns = klass.memberFunctions.map { it.name }.toSet()

    // Must have invoke
    assertTrue("invoke" in fns,
        "$label: memberFunctions must include 'invoke', got: $fns")

    // Must have the standard Object methods
    for (name in listOf("equals", "hashCode", "toString")) {
        assertTrue(name in fns,
            "$label: memberFunctions must include '$name', got: $fns")
    }
}

fun box(): String {
    checkMemberFunctions(SuspendUnit::class,           "SuspendUnit")
    checkMemberFunctions(SuspendStringProducer::class, "SuspendStringProducer")
    checkMemberFunctions(SuspendMapper::class,         "SuspendMapper")

    // Verify invoke is callable
    val invoker = SuspendStringProducer::class.memberFunctions.first { it.name == "invoke" }
    assertTrue(invoker.isSuspend)

    return "OK"
}

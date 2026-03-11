// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

import kotlin.reflect.KMutableProperty
import kotlin.test.assertEquals

inline fun check(message: String, generate: () -> Any?) {
    val x1: Any?
    val x2: Any?
    try {
        x1 = generate()

        // Force clear the internal maps, as if the weak values in them are garbage-collected.
        synchronized(kotlin.reflect.jvm.internal.ReflectionFactoryImpl::class.java) {
            kotlin.reflect.jvm.internal.ReflectionFactoryImpl.clearCaches()
        }

        x2 = generate()
    } catch (e: Throwable) {
        throw AssertionError("Fail $message", e)
    }

    assertEquals(x1, x2, "Fail equals $message")
    assertEquals(x2, x1, "Fail equals $message")
    assertEquals(x1.hashCode(), x2.hashCode(), "Fail hashCode $message")
}

class C(c: Any) {
    fun Any.a(a: Any): Any = a

    var <X> X.x: X
        get() = this
        set(value) {}
}

fun box(): String {
    check("constructor parameter") { C::class.constructors.single().parameters.single() }
    check("instance parameter") { C::class.members.single { it.name == "a" }.parameters[0] }
    check("value parameter") { C::class.members.single { it.name == "a" }.parameters[1] }

    check("extension receiver parameter") { (C::class.members.single { it.name == "x" } as KMutableProperty<*>).parameters[1] }

    // TODO: depends on KT-13490
    // check("property setter parameter") { (C::class.members.single { it.name == "x" } as KMutableProperty<*>).setter.parameters[2] }

    return "OK"
}

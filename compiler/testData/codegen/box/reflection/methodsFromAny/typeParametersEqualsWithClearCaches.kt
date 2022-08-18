// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

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

class C<T> {
    fun <V> v(): V? = null
    fun t(): T? = null
    val <U> U.u: U get() = this
}

fun <W> W.w() {}
val <X> X.x: X get() = this

fun box(): String {
    check("T from C's typeParameters") { C::class.typeParameters.single() }
    check("V from v's typeParameters") { C::class.members.single { it.name == "v" }.typeParameters.single() }

    check("V from v's returnType") { C::class.members.single { it.name == "v" }.returnType.classifier }
    check("T from t's returnType") { C::class.members.single { it.name == "t" }.returnType.classifier }
    check("U from u's parameter type") { C::class.members.single { it.name == "u" }.parameters[1].type.classifier }

    check("W from w's parameter type") { Any::w.parameters.single().type.classifier }
    check("X from x's parameter type") { Any::x.parameters.single().type.classifier }

    check("Z from J's typeParameters") { J::class.typeParameters.single() }
    check("Z from z's returnType") { J::class.members.single { it.name == "z" }.returnType.classifier }

    return "OK"
}

// FILE: J.java

public interface J<Z> {
    Z z();
}

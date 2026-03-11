// TARGET_BACKEND: JVM
// WITH_REFLECT
package test

import kotlin.test.assertEquals

interface I<A, B>

fun <S, T> f() = object : I<T, S> {}

fun box(): String {
    // Ideally, we would have `test.I<T, S>`, where `T` and `S` are types obtained from f's type parameters.
    // But currently it's not implemented: KT-47030.
    // At the moment, this test checks that kotlin-reflect at least does not throw exception on such types.
    assertEquals("[test.I<???, ???>, kotlin.Any]", f<Any, Any>()::class.supertypes.toString())

    val i = f<Any, Any>()::class.supertypes.single { it.classifier == I::class }
    val arg = i.arguments.first().type!!
    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        // In the descriptor-based implementation, classifier for error type is null.
        assertEquals(null, arg.classifier)
    } else {
        // In the new implementation, classifier is a special object which is neither a class, nor a type parameter, nor a type alias,
        // and its `toString` returns something meaningful (although it's still not a requirement and it could've returned `???` as well,
        // the main idea is that it should not throw exception).
        assertEquals("[Error type parameter 0]", arg.classifier!!.toString())
    }

    return "OK"
}

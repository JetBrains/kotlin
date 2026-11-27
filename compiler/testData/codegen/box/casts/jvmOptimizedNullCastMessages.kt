// LANGUAGE: +JvmOptimizedNullCastMessages +JvmOptimizedNullCastMessagesInInlineFunctions
// TARGET_BACKEND: JVM
// WITH_STDLIB
package test

import kotlin.test.assertEquals

private fun test(f: () -> Unit): String? =
    try {
        f()
        throw AssertionError("Fail: no NPE")
    } catch (e: NullPointerException) {
        val message = e.message!!
        val prefix = "null cannot be cast to non-null type "
        if (!message.startsWith(prefix)) {
            throw AssertionError("Fail: unexpected exception message: ${e.message}", e)
        }
        message.substringAfter(prefix)
    }

inline fun <reified T : Any> Any?.cast(): T = this as T

fun <U : Any> Any?.castUnchecked(): U = this as U
fun <U : Number> Any?.castUncheckedNumber(): U = this as U
inline fun <reified T : Any> Any?.castArray(): Array<T> = this as Array<T>

class C {
    class Nested
}

fun box(): String {
    val nil: Any? = null
    class Local

    assertEquals("test.C", test { nil as C })
    assertEquals("test.C.Nested", test { nil as C.Nested })
    assertEquals("test.JvmOptimizedNullCastMessagesKt\$box\$Local", test { nil as Local })
    assertEquals("kotlin.String", test { nil as String })
    assertEquals("kotlin.Int", test { nil as Int })
    assertEquals("kotlin.IntArray", test { nil as IntArray })
    assertEquals("kotlin.Array", test { nil as Array<Int> })
    assertEquals("kotlin.collections.List", test { nil as List<C> })
    assertEquals("kotlin.collections.List", test { nil as MutableList<C> })
    assertEquals("kotlin.Function0", test { nil as () -> Unit })

    assertEquals("kotlin.Any", test { nil.castUnchecked<Number>() })
    assertEquals("kotlin.Number", test { nil.castUncheckedNumber<Int>() })

    assertEquals("test.C", test { nil.cast<C>() })
    assertEquals("test.C.Nested", test { nil.cast<C.Nested>() })
    assertEquals("kotlin.String", test { nil.cast<String>() })
    assertEquals("kotlin.Int", test { nil.cast<Int>() })
    assertEquals("kotlin.IntArray", test { nil.cast<IntArray>() })
    assertEquals("kotlin.Array", test { nil.cast<Array<Int>>() })
    assertEquals("kotlin.Array", test { nil.castArray<C>() })
    assertEquals("kotlin.collections.List", test { nil.cast<List<C>>() })
    assertEquals("kotlin.collections.List", test { nil.cast<MutableList<C>>() })

    return "OK"
}

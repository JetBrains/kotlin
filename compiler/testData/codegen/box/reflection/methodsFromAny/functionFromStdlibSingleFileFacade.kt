// KT-12630 KotlinReflectionInternalError on referencing some functions from stdlib

// IGNORE_BACKEND: JS
// WITH_REFLECT

import kotlin.test.*

fun box(): String {
    val lazyOf: (String) -> Lazy<String> = ::lazyOf
    assertEquals("fun lazyOf(T): kotlin.Lazy<T>", lazyOf.toString())

    return "OK"
}

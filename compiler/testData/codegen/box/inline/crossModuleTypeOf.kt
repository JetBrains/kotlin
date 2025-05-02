// ISSUE: KT-77170
// WTIH_STDLIB
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

// MODULE: lib
// FILE: lib.kt
import kotlin.reflect.*

inline fun <reified T> foo() = typeOf<T>()
inline fun <reified T: Any> bar() : KClass<T> = T::class

// MODULE: main(lib)
// FILE: main.kt

import kotlin.reflect.*

class O
class K

fun box() = (foo<O>().classifier as KClass<*>).simpleName + bar<K>().simpleName

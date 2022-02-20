// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KClass

class OK

class T

inline fun <reified F : Any> bar(k: KClass<out F>): String = k.simpleName!!
inline fun <reified T : Any> foo(): String = bar(T::class)

fun box(): String {
    return foo<OK>()
}

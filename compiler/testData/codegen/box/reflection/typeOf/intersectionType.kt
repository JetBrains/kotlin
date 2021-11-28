// IGNORE_BACKEND: JS
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

import kotlin.reflect.typeOf

class Inv<T>(val v: T)

interface X
interface Y

object A : X, Y
object B : X, Y

fun <T> sel(a: T, b: T) = a

inline fun <reified T> T.valueTypeOf() = typeOf<T>()

fun box(): String {
    val t = sel(Inv(A), Inv(B)).v.valueTypeOf()
    return if (t == typeOf<Any>()) "OK" else "Fail: $t"
}

// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt
import kotlin.reflect.KType
import kotlin.reflect.typeOf

fun foo(a: Any?, b: KType): KType = b

inline fun <reified T> namedTypeOf(x: Any?): KType = foo(b = typeOf<T>(), a = x)

// MODULE: main(lib)
// FILE: main.kt
import kotlin.reflect.typeOf

fun box(): String {
    val t = namedTypeOf<String>(1)
    return if (t == typeOf<String>()) "OK" else "Fail: $t"
}

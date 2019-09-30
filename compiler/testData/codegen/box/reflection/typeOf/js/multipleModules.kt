// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JS
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

// MODULE: lib1
// FILE: lib1.kt
import kotlin.reflect.typeOf

inline fun <reified T> get() = typeOf<T>()

// MODULE: lib2(lib1)
// FILE: lib2.kt
inline fun <reified U> get1() = get<U?>()

// MODULE: lib3(lib1, lib2)
// FILE: lib3.kt
import kotlin.reflect.KType

inline fun <reified V> get2(): KType {
    return get1<Map<in V?, Array<V>>>()
}

// MODULE: main(lib1, lib2, lib3)
// FILE: lib4.kt
import kotlin.test.assertEquals

interface C

fun box(): String {
    assertEquals("C?", get1<C>().toString())
    assertEquals("Map<in C?, Array<C>>?", get2<C>().toString())
    assertEquals("Map<in List<C>?, Array<List<C>>>?", get2<List<C>>().toString())

    assertEquals("Short?", get1<Short>().toString())
    assertEquals("Map<in Short?, Array<Short>>?", get2<Short>().toString())
    assertEquals("Map<in List<Short>?, Array<List<Short>>>?", get2<List<Short>>().toString())
    assertEquals("Map<in List<dynamic>?, Array<List<dynamic>>>?", get2<List<dynamic>>().toString())

    return "OK"
}

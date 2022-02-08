// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.reflect.*

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Runtime

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class Binary

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Source

inline fun <reified X> f() = g<List<X>>()
inline fun <reified Y> g() = typeOf<Y>()

inline fun <reified Z> a() = typeOf<@Runtime Z>()

inline fun <reified R> test() {
    check(typeOf<@Runtime R.() -> Unit>())
    check(typeOf<R.() -> Unit>())
}

fun check(type: KType) {
    if (type.annotations.isNotEmpty()) {
        error("KType.annotations should be empty: ${type.annotations}")
    }
}

fun box(): String {
    check(typeOf<String.() -> Int>())
    check(f<Int.(Int) -> Unit>())

    check(typeOf<@Runtime Int.() -> List<String>>())
    check(f<@Runtime Unit.() -> Array<*>>())

    check(typeOf<@Runtime String>())
    check(f<@Runtime String>())
    check(typeOf<@Binary String>())
    check(f<@Binary String>())
    check(typeOf<@Source String>())
    check(f<@Source String>())

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    check(typeOf<@kotlin.internal.Exact String>())
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    check(f<@kotlin.internal.Exact String>())

    check(typeOf<Map<String, List<@Runtime Int>>>())
    check(f<Map<String, List<@Runtime Int>>>())

    check(a<String>())

    test<String>()

    return "OK"
}

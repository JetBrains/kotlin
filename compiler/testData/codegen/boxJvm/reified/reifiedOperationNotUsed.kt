// ISSUE: KT-34051

// MODULE: a
// FILE: A.kt

inline fun <reified T> javaClass() { T::class }

inline fun <reified T> typeOf() { kotlin.reflect.typeOf<T>() }

// MODULE: test(a)
// FILE: test.kt

fun box(): String {
    javaClass<String>()
    typeOf<Int>()
    return "OK"
}

// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// !LANGUAGE: +ProhibitNonReifiedArraysAsReifiedTypeArguments

import kotlin.reflect.typeOf

inline fun <X, reified Y, Z : Y> test1() {
    typeOf<X>()
    typeOf<List<X>>()
    typeOf<Array<X?>>()

    typeOf<Y>()

    typeOf<Z>()
    typeOf<List<Z>?>()
    typeOf<Array<Z>>()
}


class Test2<W> {
    fun test2() {
        typeOf<W>()
        typeOf<List<W?>>()
        typeOf<Array<W>>()
    }
}


inline fun <reified U> f() {
    typeOf<U>()
}

fun <T> test3() {
    // We don't report anything here because we can't know in frontend how the corresponding type parameter is used in f
    f<List<T>>()
}

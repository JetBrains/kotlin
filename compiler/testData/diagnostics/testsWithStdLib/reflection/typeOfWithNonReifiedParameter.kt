// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi

import kotlin.reflect.typeOf

inline fun <X, reified Y, Z : Y> test1() {
    <!UNSUPPORTED!>typeOf<!><<!TYPE_PARAMETER_AS_REIFIED!>X<!>>()
    <!UNSUPPORTED!>typeOf<!><List<X>>()
    <!UNSUPPORTED!>typeOf<!><Array<X?>>()

    typeOf<Y>()

    <!UNSUPPORTED!>typeOf<!><<!TYPE_PARAMETER_AS_REIFIED!>Z<!>>()
    <!UNSUPPORTED!>typeOf<!><List<Z>?>()
    <!UNSUPPORTED!>typeOf<!><Array<Z>>()
}


class Test2<W> {
    fun test2() {
        <!UNSUPPORTED!>typeOf<!><<!TYPE_PARAMETER_AS_REIFIED!>W<!>>()
        <!UNSUPPORTED!>typeOf<!><List<W?>>()
        <!UNSUPPORTED!>typeOf<!><Array<W>>()
    }
}


inline fun <reified U> f() {
    typeOf<U>()
}

fun <T> test3() {
    // We don't report anything here because we can't know in frontend how the corresponding type parameter is used in f
    f<List<T>>()
}

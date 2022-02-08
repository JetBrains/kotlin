// WITH_STDLIB
// USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi

import kotlin.reflect.*

inline fun <reified X> f() = g<List<X>>()
inline fun <reified Y> g() = typeOf<Y>()

fun test() {
    <!TYPEOF_SUSPEND_TYPE!>typeOf<suspend () -> Int>()<!>
    <!TYPEOF_SUSPEND_TYPE!>f<suspend (String) -> Unit>()<!>

    <!TYPEOF_SUSPEND_TYPE!>typeOf<suspend Int.() -> List<String>>()<!>
    <!TYPEOF_SUSPEND_TYPE!>f<suspend Unit.() -> Array<*>>()<!>
}

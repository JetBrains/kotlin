// WITH_RUNTIME
// USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JVM_IR

import kotlin.reflect.*

inline fun <reified X> f() = g<List<X>>()
inline fun <reified Y> g() = typeOf<Y>()

inline fun <reified Z> a() = typeOf<@Runtime Z>()

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Runtime

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class Binary

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class Source

fun test() {
    <!TYPEOF_EXTENSION_FUNCTION_TYPE!>typeOf<String.() -> Int>()<!>
    <!TYPEOF_EXTENSION_FUNCTION_TYPE!>f<Int.(Int) -> Unit>()<!>

    <!TYPEOF_EXTENSION_FUNCTION_TYPE, TYPEOF_SUSPEND_TYPE!>typeOf<suspend Int.() -> List<String>>()<!>
    <!TYPEOF_EXTENSION_FUNCTION_TYPE, TYPEOF_SUSPEND_TYPE!>f<suspend Unit.() -> Array<*>>()<!>

    <!TYPEOF_EXTENSION_FUNCTION_TYPE!>typeOf<@Runtime Int.() -> List<String>>()<!>
    <!TYPEOF_EXTENSION_FUNCTION_TYPE!>f<@Runtime Unit.() -> Array<*>>()<!>

    <!TYPEOF_ANNOTATED_TYPE!>typeOf<@Runtime String>()<!>
    <!TYPEOF_ANNOTATED_TYPE!>f<@Runtime String>()<!>
    <!TYPEOF_ANNOTATED_TYPE!>typeOf<@Binary String>()<!>
    <!TYPEOF_ANNOTATED_TYPE!>f<@Binary String>()<!>
    <!TYPEOF_ANNOTATED_TYPE!>typeOf<@Source String>()<!>
    <!TYPEOF_ANNOTATED_TYPE!>f<@Source String>()<!>

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    <!TYPEOF_ANNOTATED_TYPE!>typeOf<@kotlin.internal.Exact String>()<!>
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    <!TYPEOF_ANNOTATED_TYPE!>f<@kotlin.internal.Exact String>()<!>

    <!TYPEOF_ANNOTATED_TYPE!>typeOf<Map<String, List<@Runtime Int>>>()<!>
    <!TYPEOF_ANNOTATED_TYPE!>f<Map<String, List<@Runtime Int>>>()<!>

    // TODO: https://youtrack.jetbrains.com/issue/KT-29919#focus=Comments-27-5065356.0-0
    a<String>()

    test2<String>()
}

inline fun <reified R> test2() {
    <!TYPEOF_EXTENSION_FUNCTION_TYPE!>typeOf<@Runtime R.()->Unit>()<!>
    <!TYPEOF_EXTENSION_FUNCTION_TYPE!>typeOf<R.()->Unit>()<!>
}

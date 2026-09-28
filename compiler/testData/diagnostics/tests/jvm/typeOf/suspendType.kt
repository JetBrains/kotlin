// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

import kotlin.reflect.*

inline fun <reified X> f() = g<List<X>>()
inline fun <reified Y> g() = typeOf<Y>()

fun test() {
    <!TYPEOF_SUSPEND_TYPE!>typeOf<suspend () -> Int>()<!>
    <!TYPEOF_SUSPEND_TYPE!>f<suspend (String) -> Unit>()<!>

    <!TYPEOF_SUSPEND_TYPE!>typeOf<suspend Int.() -> List<String>>()<!>
    <!TYPEOF_SUSPEND_TYPE!>f<suspend Unit.() -> Array<*>>()<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, nullableType, reified, starProjection, suspend,
typeParameter, typeWithExtension */

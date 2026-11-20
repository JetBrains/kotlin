// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `TYPEOF_SUSPEND_TYPE`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

import kotlin.reflect.*

inline fun <reified X> f() = g<List<X>>()
inline fun <reified Y> g() = typeOf<Y>()

fun test() {
    typeOf<suspend () -> Int>()
    f<suspend (String) -> Unit>()

    typeOf<suspend Int.() -> List<String>>()
    f<suspend Unit.() -> Array<*>>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, nullableType, reified, starProjection, suspend,
typeParameter, typeWithExtension */

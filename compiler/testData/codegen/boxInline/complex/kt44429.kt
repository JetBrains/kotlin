// IGNORE_BACKEND: WASM_JS, WASM_WASI
// FREE_COMPILER_ARGS: -Xbinary=preCodegenInlineThreshold=0
// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=NO && cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: optimizationMode=NO && cacheMode=STATIC_PER_FILE_EVERYWHERE
// https://youtrack.jetbrains.com/issue/KT-44571/Segfault-on-unnecessary-int-unboxing
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:*
// ^^^ Native Klib compatibility tests are executed in DEBUG mode, so this testcase should be muted.
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun <T> takeT(t: T) {}
inline fun <T : Any> takeTSuperAny(t: T) {}
inline fun <T, U : T> takeU(u: U) {}
inline fun <T, U : T, R : U> takeR(r: R) {}
inline fun <A, B, T : Map<A, List<B>>> takeTWithMap(t: T) {}

// FILE: 2.kt
import test.*

fun box(): String {
    val f = { null } as () -> Int
    takeT(f())
    // Without fix we are going to get following instructions
    //    CHECKCAST java/lang/Number
    //    INVOKEVIRTUAL java/lang/Number.intValue ()I    // <- this one leads to NPE
    takeTSuperAny(f())
    takeU(f())
    takeR(f())

    val g = { null } as () -> Map<Int, List<String>>
    takeTWithMap(g())
    return "OK"
}

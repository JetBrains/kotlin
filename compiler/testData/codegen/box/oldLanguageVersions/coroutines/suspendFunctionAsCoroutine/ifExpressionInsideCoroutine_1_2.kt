// !LANGUAGE: -ReleaseCoroutines
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS
// WITH_RUNTIME
// WITH_COROUTINES
// DONT_TARGET_EXACT_BACKEND: JS_IR

import helpers.*
import kotlin.coroutines.experimental.*

val f = run {
    buildSequence {
        if (true) {
            yield("OK")
        }
    }.toList()
}

fun box(): String {
    return f[0]
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ experimental 

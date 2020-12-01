// WITH_RUNTIME

// Need to ignore dexing for now. This generates invalid inner-class attributes on the IR backend.
//
// java.lang.AssertionError: D8 dexing error: D8 dexing info: Malformed inner-class attribute:
//	outerTypeInternal: C$result$1
//	innerTypeInternal: C$no_name_in_PSI_3d19d79d_1ba9_4cd0_b7f5_b46aa3cd5d40$WhenMappings
//	innerName: WhenMappings
//
// IGNORE_DEXING

import kotlin.coroutines.*

fun launch(block: suspend () -> String): String {
    var result = ""
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}

enum class E { A }

class C(val e: E) {
    val result = launch {
        when (e) {
            E.A -> "OK"
        }
    }
}

fun box(): String = C(E.A).result

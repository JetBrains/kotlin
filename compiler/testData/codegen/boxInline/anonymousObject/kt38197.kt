// WITH_COROUTINES
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-62464

// FILE: test.kt

var result = "Fail"

fun use(token: String) {
    result = token
}

inline fun f(crossinline body: suspend () -> Unit) =
    g<
        Any, Any, Any, Any, Any, Any, Any, Any, Any, Any,
        Any, Any, Any, Any, Any, Any, Any, Any, Any, Any,
    >(body)

inline fun <
    reified U01, reified U02, reified U03, reified U04, reified U05, reified U06, reified U07, reified U08, reified U09, reified U10,
    reified U11, reified U12, reified U13, reified U14, reified U15, reified U16, reified U17, reified U18, reified U19, reified U20,
> g(crossinline body: suspend () -> Unit): suspend () -> Unit {
    return run {
        run {
            run {
                run {
                    run {
                        run {
                            run {
                                run {
                                    suspend {
                                        body()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// FILE: box.kt

import kotlin.coroutines.*
import helpers.*

fun box(): String {
    var token = "OK"
    f {
        use(token)
    }.startCoroutine(EmptyContinuation)
    return result
}

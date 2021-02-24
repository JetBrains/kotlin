// !LANGUAGE: +ReleaseCoroutines
// SKIP_TXT

import kotlin.suspend as suspendLambda

fun bar() {
    suspend {
        println()
    }

    kotlin.suspend {

    }

    suspendLambda {
        println()
    }

    suspendLambda() {
        println()
    }

    suspendLambda({ println() })

    suspendLambda<Unit> {
        println()
    }

    val w: (suspend () -> Int) -> Any? = ::suspendLambda
}

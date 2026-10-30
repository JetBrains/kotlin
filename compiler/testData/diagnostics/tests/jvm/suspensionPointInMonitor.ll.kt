// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `SUSPENSION_POINT_INSIDE_MONITOR`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

val lock = Any()

inline fun inlineMe(c: () -> Unit) {
    synchronized(lock) {
        c()
    }
}

inline fun monitorInFinally(a: () -> Unit, b: () -> Unit) {
    try {
        a()
    } finally {
        synchronized(lock) {
            b()
        }
    }
}

// MODULE: main(lib)
// FILE: main.kt

fun builder(c: suspend () -> Unit) {}

suspend fun suspensionPoint() {}

fun test() {
    builder {
        inlineMe {
            suspensionPoint()
        }
    }

    builder {
        monitorInFinally(
            {},
            { suspensionPoint() }
        )
    }

    builder {
        withCrossinline {}

        withCrossinline {
            suspensionPoint()
        }
    }

    synchronized(lock) {
        builder {
            suspensionPoint()
        }
    }

    synchronized(lock) {
        object : SuspendRunnable {
            override suspend fun run() {
                suspensionPoint()
            }
        }
    }
}

interface SuspendRunnable {
    suspend fun run()
}


inline fun withCrossinline(crossinline a: suspend () -> Unit): suspend () -> Unit {
    val c : suspend () -> Unit = {
        inlineMe {
            a()
        }
    }
    return c
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, crossinline, functionDeclaration, functionalType, inline,
interfaceDeclaration, lambdaLiteral, localProperty, override, propertyDeclaration, suspend, tryExpression */

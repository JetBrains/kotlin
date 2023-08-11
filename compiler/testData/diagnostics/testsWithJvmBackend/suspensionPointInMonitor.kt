// !DIAGNOSTICS: -UNUSED_PARAMETER
// FIR_IDENTICAL
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
    builder <!SUSPENSION_POINT_INSIDE_MONITOR!>{
        inlineMe {
            suspensionPoint()
        }
    }<!>

    builder <!SUSPENSION_POINT_INSIDE_MONITOR!>{
        monitorInFinally(
            {},
            { suspensionPoint() }
        )
    }<!>

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


<!SUSPENSION_POINT_INSIDE_MONITOR!>inline fun withCrossinline(crossinline a: suspend () -> Unit): suspend () -> Unit {
    val c : suspend () -> Unit = <!SUSPENSION_POINT_INSIDE_MONITOR!>{
        inlineMe {
            a()
        }
    }<!>
    return c
}<!>

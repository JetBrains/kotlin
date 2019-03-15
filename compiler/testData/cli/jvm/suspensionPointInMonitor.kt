fun builder(c: suspend () -> Unit) {}

private val lock = Any()

suspend fun suspensionPoint() {}

private inline fun inlineMe(c: () -> Unit) {
    synchronized(lock) {
        c()
    }
}

private inline fun monitorInFinally(a: () -> Unit, b: () -> Unit) {
    try {
        a()
    } finally {
        synchronized(lock) {
            b()
        }
    }
}

fun test() {
    builder {
        synchronized(lock) {
            suspensionPoint()
        }
    }

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

    object : SuspendRunnable {
        override suspend fun run() {
            synchronized(lock) {
                suspensionPoint()
            }
        }
    }
}

interface SuspendRunnable {
    suspend fun run()
}

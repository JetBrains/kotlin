fun builder(c: suspend () -> Unit) {}

private val lock = Any()

suspend fun suspensionPoint() {}

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

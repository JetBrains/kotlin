fun builder(c: suspend () -> Unit) {}

private val lock = Any()

suspend fun suspensionPoint() {}

fun test() {
    builder {
        synchronized(lock) {
            suspensionPoint()
        }

        inlineMe {
            suspensionPoint()
        }

        monitorInFinally(
            { suspensionPoint() },
            { suspensionPoint() }
        )
    }
}

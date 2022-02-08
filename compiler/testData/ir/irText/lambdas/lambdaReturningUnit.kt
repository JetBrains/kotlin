// With FIR the type of the lambda in box is () -> Any?
// With old frontend the type of the lambda is () -> Unit

inline fun flaf(block: () -> Any?) {
    block()
}

suspend fun box() {
    flaf {
        Unit
    }
}

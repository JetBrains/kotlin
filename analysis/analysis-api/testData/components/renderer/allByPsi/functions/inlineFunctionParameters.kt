inline fun withCallbacks(
    crossinline onStart: () -> Unit,
    noinline onEnd: () -> Unit,
    direct: () -> Unit,
) {}

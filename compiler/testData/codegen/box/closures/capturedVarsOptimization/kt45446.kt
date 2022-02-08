
// This test checks that bytecode optimizer doesn't crash on unreachable code.

fun box(): String {
    try {
        remove()
    }
    catch (e: Exception) {
        return e.message!!
    }
    return "Should fail with exception"
}

fun remove() {
    throw Exception("OK")
    var captured = 0
    debug {
        captured = 1
    }
}

private fun debug(f: () -> Unit) {
    f()
}

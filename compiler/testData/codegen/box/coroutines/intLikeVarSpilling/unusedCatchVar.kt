// WITH_STDLIB
// WITH_COROUTINES
// FULL_RUNTIME

class JarFile {
    fun entries() = listOf<String>()
}

private fun scriptTemplatesDiscoverySequence(): Sequence<String> {
    return sequence<String> {
        yield("OK")
        for (dep in listOf<String>()) {
            try {
            } catch (e: Throwable) {
            }
        }
    }
}

fun box(): String {
    return scriptTemplatesDiscoverySequence().first()
}

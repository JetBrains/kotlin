// WITH_RUNTIME
// WITH_COROUTINES
// FULL_RUNTIME
// KJS_WITH_FULL_RUNTIME

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

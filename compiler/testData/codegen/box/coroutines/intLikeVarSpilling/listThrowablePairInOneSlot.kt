// WITH_STDLIB
// WITH_COROUTINES
// FULL_RUNTIME

class JarFile {
    fun entries() = listOf<String>()
}

private fun scriptTemplatesDiscoverySequence(): Sequence<String> {
    return sequence<String> {
        for (dep in listOf<String>()) {
            try {
                val jar = JarFile()
                try {
                    jar.entries().map { it }
                    val (loadedDefinitions, notFoundClasses) = listOf<String>() to listOf<String>()
                    listOf<String>().forEach {
                        yield(it)
                    }
                } catch (e: Throwable) {
                    e.message
                }
            } catch (e: Exception) {
                e.message
            }
        }
        yield("OK")
    }
}

fun box(): String {
    return scriptTemplatesDiscoverySequence().first()
}

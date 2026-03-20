// TARGET_BACKEND: JVM
// FULL_JDK
// ISSUE: KT-84931

private inline fun <reified E> transformArguments(arguments: MutableList<E>) {
    arguments.replaceAll { it }
}

fun box(): String {
    mutableListOf(null, 1).replaceAll { it }

    transformArguments(mutableListOf(null, 2))

    return "OK"
}

// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

@OptIn(ExperimentalStdlibApi::class)
fun getAllPossibleNames(subScopes: List<List<String>>): Set<String> = withValidityAssertion {
    buildSet {
        subScopes.flatMapTo(this) { it }
    }
}

inline fun <R> withValidityAssertion(action: () -> R): R {
    return action()
}

fun box(): String {
    return getAllPossibleNames(listOf(listOf("O"), listOf("K"))).joinToString("")
}

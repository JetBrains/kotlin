// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

val f = listOf("").<error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /firstOrNull">firstOrNull</error>(1)

fun <T> listOf(element: T): List<T> = java.util.Collections.singletonList(element)
fun <T> Iterable<T>.firstOrNull(predicate: (T) -> Boolean): T? = null

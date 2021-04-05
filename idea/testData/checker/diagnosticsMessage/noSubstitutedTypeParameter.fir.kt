// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

val f = listOf("").firstOrNull(<error descr="[ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is kotlin/Int but kotlin/Function1<TypeVariable(T), kotlin/Boolean> was expected">1</error>)

fun <T> listOf(element: T): List<T> = java.util.Collections.singletonList(element)
fun <T> Iterable<T>.firstOrNull(<warning>predicate</warning>: (T) -> Boolean): T? = null
val f = listOf("").<error descr="[TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR] Type inference failed: fun <T> Iterable<T>.firstOrNull(predicate: (T) -> Boolean): T?
cannot be applied to
receiver: List<String>  arguments: (Int)
">firstOrNull</error>(<error descr="[CONSTANT_EXPECTED_TYPE_MISMATCH] The integer literal does not conform to the expected type (String) -> Boolean">1</error>)

fun <T> listOf(element: T): List<T> = java.util.Collections.singletonList(element)
fun <T> Iterable<T>.firstOrNull(<warning>predicate</warning>: (T) -> Boolean): T? = null
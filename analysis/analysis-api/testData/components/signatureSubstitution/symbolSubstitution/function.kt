// the test checks that function own type parameters (`S` in this test) won't be substituted recursively

// SUBSTITUTOR: T -> kotlin.collections.List<S>, S -> kotlin.Long

fun <T, S> f<caret>oo(x: List<T>, y: Map<T, List<S>>, k: String): T
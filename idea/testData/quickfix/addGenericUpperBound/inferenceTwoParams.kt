// "Add 'kotlin.Any' as upper bound for E" "true"
// ERROR: Type parameter bound for U in fun <T : Any, U : Any> foo(x: T, y: U): Int<br> is not satisfied: inferred type F is not a subtype of Any

fun <T : Any, U: Any> foo(x: T, y: U) = 1

fun <E, F> bar(x: E, y: F) = <caret>foo(x, y)

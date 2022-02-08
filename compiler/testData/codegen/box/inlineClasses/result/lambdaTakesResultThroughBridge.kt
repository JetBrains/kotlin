// WITH_STDLIB
fun <T> Result<T>.getOrNullNoinline() = getOrNull()

val x = { a: Int, b: Result<String> -> b.getOrNullNoinline() }

fun box() = x(1, Result.success("OK"))

// ADDITIONAL_COMPILER_ARGUMENTS: -Xcoroutines=enable

fun f(g: suspend () -> Unit): Any = g

suspend fun h() = f { }

header suspend fun k()

header fun l(g: suspend () -> Unit): Any

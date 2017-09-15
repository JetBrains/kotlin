// ADDITIONAL_COMPILER_ARGUMENTS: -Xcoroutines=enable

fun f(g: suspend () -> Unit): Any = g

suspend fun h() = f { }

expect suspend fun k()

expect fun l(g: suspend () -> Unit): Any

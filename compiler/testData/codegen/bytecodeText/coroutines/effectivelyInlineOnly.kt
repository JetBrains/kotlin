// WITH_COROUTINES
// TREAT_AS_ONE_FILE
inline suspend fun <reified T> f(x: suspend () -> T) = x()
// 0 InlineMarker

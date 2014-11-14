// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_PARAMETER_AS_REIFIED

public data class Pair<out A, out B>()

inline fun <R, reified T> f(): Pair<Array<R>, Array<T>> = throw UnsupportedOperationException()

fun <A> test(): Pair<Array<String>, Array<A>> = f()

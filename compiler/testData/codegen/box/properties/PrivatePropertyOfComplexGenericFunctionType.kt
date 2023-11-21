// ISSUE: KT-63654
// WITH_STDLIB

data class State<S, out A>(private val action: (S) -> Pair<A, S>) {
    fun <B> flatMap(f: (A) -> State<S, B>): State<S, B> =
        State { s0: S ->
            val (a, s1) = action(s0)
            f(a).action(s1)
        }
}

fun box(): String {
    State<String, Int> { 42 to "" }.flatMap { i -> State { i.toLong() to "" } }
    return "OK"
}

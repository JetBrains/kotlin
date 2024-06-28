// ISSUE: KT-25668
suspend fun SequenceScope<Int>.bar() = yield(1)

fun test() {
    val seq = sequence {
        val f: suspend SequenceScope<Int>.() -> Unit = SequenceScope<Int>::bar
        f()
    }
    seq.toList()
}

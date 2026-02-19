/**
 * [T.fo<caret_1>xAny]
 * [T.fox<caret_2>AnyNullable]
 * [T.fox<caret_3>Int]
 * [T.fox<caret_4>Seq]
 * [T.fox<caret_5>SeqNullable]
 */
fun <T: CharSequence> foo() {
    val x = (null as T)
    /*
        x::foxAny // CORRECT
        x::foxAnyNullable // CORRECT
        x::foxInt // INCORRECT
        x::foxSeq // CORRECT
        x::foxSeqNullable // CORRECT
     */
}

/**
 * [T.fo<caret_6>xAny]
 * [T.fox<caret_7>AnyNullable]
 * [T.fox<caret_8>Int]
 * [T.fox<caret_9>Seq]
 * [T.fox<caret_10>SeqNullable]
 */
fun <T: CharSequence?> fooNullable() {
    val x = (null as T)
    /*
        x::foxAny // PARTIALLY CORRECT (UNSAFE_CALL)
        x::foxAnyNullable // CORRECT
        x::foxInt // INCORRECT
        x::foxSeq // PARTIALLY CORRECT (UNSAFE_CALL)
        x::foxSeqNullable // CORRECT
     */
}

/**
 * [T.foxRecu<caret_11>rsive]
 */
fun <T, S, R, L> fooComplex() where R : Iterable<L>, T: S, S : R, L: T {
    val x = (null as T)
    /*
        x::foxRecursive // CORRECT
    */
}

fun Any.foxAny() {}
fun Any?.foxAnyNullable() {}
fun Int.foxInt() {}
fun CharSequence.foxSeq() {}
fun CharSequence?.foxSeqNullable() {}
fun <T: Iterable<T>> T.foxRecursive() {}
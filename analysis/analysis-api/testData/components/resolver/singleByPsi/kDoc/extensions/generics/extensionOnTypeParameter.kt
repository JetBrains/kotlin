/**
 * [T.fo<caret_1>xAny]
 * [T.fox<caret_2>AnyNullable]
 * [T.fox<caret_3>Int]
 * [T.fox<caret_4>Seq]
 * [T.fox<caret_5>SeqNullable]
 */
fun <T> foo() {
    val x = (null as T)
    /*
        x::foxAny // PARTIALLY CORRECT (UNSAFE_CALL)
        x::foxAnyNullable // CORRECT
        x::foxInt // INCORRECT
        x::foxSeq // INCORRECT
        x::foxSeqNullable // INCORRECT
    */
}

fun Any.foxAny() {}
fun Any?.foxAnyNullable() {}
fun Int.foxInt() {}
fun CharSequence.foxSeq() {}
fun CharSequence?.foxSeqNullable() {}
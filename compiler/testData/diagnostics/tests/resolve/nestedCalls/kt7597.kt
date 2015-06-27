interface Inv<I>

fun <S, T: S> Inv<T>.reduce2(): S = null!!

fun test(a: Inv<Int>): Int {
    val b = 1 + a.reduce2()
    return b
}
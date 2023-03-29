// FIR_DUMP

fun <A : Comparable<A>> arrayData(vararg values: A): A = null!!

fun <A> arrayDataNoBound(vararg values: A): A = null!!

fun test(b: Byte) {
    select(arrayData(1), b)
    select(id(1), b)
    select(id(arrayData(1)), b)
    select(arrayDataNoBound(1), b)
}

fun <S> select(a: S, b: S) = a

fun <I : Comparable<I>> id(arg: I) = arg


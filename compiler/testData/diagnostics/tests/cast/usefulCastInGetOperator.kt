// ISSUE: KT-23873
// WITH_STDLIB
data class Holder<T>(val data: T)

val data: Map<Holder<Holder<*>>, Int> = null!!

val test = Holder(Holder(1))

fun test_1() {
    data.get(test as Holder<*>)
}

fun test_2() {
    data[test <!USELESS_CAST!>as Holder<*><!>]
}

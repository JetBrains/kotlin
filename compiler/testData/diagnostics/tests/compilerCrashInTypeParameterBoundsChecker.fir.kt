// ISSUE: KT-64644
// WITH_STDLIB

typealias MaybePair = Pair<Int, Int>?

fun <T: <!FINAL_UPPER_BOUND!>MaybePair<!>> foo(x: T) {
    if (x != null) {
        println(x<!UNSAFE_CALL!>.<!>first)
        println(x<!UNSAFE_CALL!>.<!>second)
    }
}

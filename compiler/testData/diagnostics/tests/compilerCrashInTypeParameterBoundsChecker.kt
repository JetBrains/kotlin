// ISSUE: KT-64644
// WITH_STDLIB

typealias MaybePair = Pair<Int, Int>?

fun <T: MaybePair> foo(x: T) {
    if (x != null) {
        println(<!DEBUG_INFO_SMARTCAST!>x<!>.first)
        println(<!DEBUG_INFO_SMARTCAST!>x<!>.second)
    }
}

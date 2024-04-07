// WITH_STDLIB
// SKIP_TEXT
// ISSUE: KT-29559

fun consume(i: Int) {}
fun consume(c: Char) {}
fun consume(a: Any?) {}

fun test1() {
    var a: Any

    a = 1
    consume(<!DEBUG_INFO_SMARTCAST!>a<!>)

    a = 1
    consume(<!TYPE_MISMATCH!><!DEBUG_INFO_SMARTCAST!>a<!>++<!>)

    a = 1
    consume(++<!DEBUG_INFO_SMARTCAST!>a<!>)

    a = 't'
    consume(<!TYPE_MISMATCH!><!DEBUG_INFO_SMARTCAST!>a<!>++<!>)

    a = 't'
    consume(++<!DEBUG_INFO_SMARTCAST!>a<!>)
    consume(<!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>a<!>++<!>)
}

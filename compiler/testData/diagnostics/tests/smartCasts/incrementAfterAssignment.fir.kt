// WITH_STDLIB
// SKIP_TEXT
// ISSUE: KT-29559

fun consume(i: Int) {}
fun consume(c: Char) {}
fun consume(a: Any?) {}

fun test1() {
    var a: Any

    a = 1
    consume(a)

    a = 1
    consume(a++)

    a = 1
    consume(++a)

    a = 't'
    consume(a++)

    a = 't'
    consume(++a)
    consume(a++)
}

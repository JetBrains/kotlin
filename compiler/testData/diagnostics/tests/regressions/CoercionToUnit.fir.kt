fun foo(u : Unit) : Int = 1

fun test() : Int {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1)
    val a : () -> Unit = {
        <!INAPPLICABLE_CANDIDATE!>foo<!>(1)
    }
    return 1
}

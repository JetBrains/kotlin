// !WITH_NEW_INFERENCE
fun foo(n: Number) = n

fun test() {
    <!INAPPLICABLE_CANDIDATE!>foo<!>('a')
    
    val c = 'c'
    <!INAPPLICABLE_CANDIDATE!>foo<!>(c)

    val d: Char? = 'd'
    <!INAPPLICABLE_CANDIDATE!>foo<!>(d!!)
}

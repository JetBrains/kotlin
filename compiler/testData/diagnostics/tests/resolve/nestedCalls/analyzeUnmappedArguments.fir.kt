package c

fun test() {
    with (1) l@ {
        <!INAPPLICABLE_CANDIDATE!>foo<!>(1, zz = { this@l } )
    }
}

fun foo(x: Int) = x

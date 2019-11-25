fun foo(func: (Int).Int -> Int) {}

fun test() {
    <!INAPPLICABLE_CANDIDATE!>foo<!> {
        this + <!UNRESOLVED_REFERENCE!>it<!>
    }
}
class Foo(vararg val strings: String)

fun test_1(foo: Foo) {
    <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for (s in foo.strings) {
        s.<!UNRESOLVED_REFERENCE!>length<!>
    }<!>
}

fun test_2(vararg strings: String) {
    for (s in strings) {
        s.length
    }
}

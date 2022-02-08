fun foo(first: String) {}
fun foo2(first: String, second: Int) {}

fun test() {
    val int = 1
    foo(<!ARGUMENT_TYPE_MISMATCH!>int<!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>2<!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>run { 20 }<!>)

    foo2("asdf", 3)
    foo2(<!ARGUMENT_TYPE_MISMATCH!>4<!>, <!ARGUMENT_TYPE_MISMATCH!>"asdf"<!>)
    foo2(<!ARGUMENT_TYPE_MISMATCH!>5<!>, 6)
}

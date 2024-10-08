fun main() {
    val foo = <!UNSUPPORTED!>[1]<!>
    acceptList(<!TYPE_MISMATCH!>foo<!>)
}

fun acceptList(s: List<Int>) {}

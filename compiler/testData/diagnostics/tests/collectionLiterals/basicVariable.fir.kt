fun main() {
    val foo = [1]
    acceptList(<!ARGUMENT_TYPE_MISMATCH!>foo<!>)
}

fun acceptList(s: List<Int>) {}

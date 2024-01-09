fun foo() {
    val ext: String.(Int) -> Unit

    val usedReceiver = "foo"

    val <!UNUSED_VARIABLE!>unusedReceiver<!> = "bar"

    usedReceiver.ext(10)
}

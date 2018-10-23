class Some {
    fun bar() {}
}

fun Some?.foo() {
    <caret>if (((this) != null)) {
        bar()
    }
}
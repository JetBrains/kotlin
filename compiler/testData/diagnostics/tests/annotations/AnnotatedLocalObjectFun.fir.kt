annotation class My

fun foo() {
    val s = object {
        @My fun bar() {}
    }
    s.bar()
}

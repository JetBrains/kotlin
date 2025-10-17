fun interface Foo<T> {
    fun invoke(): T
}

fun test() {
    Foo { }
    Foo<Unit> { }
    Foo<String> <!ARGUMENT_TYPE_MISMATCH!>{ }<!>
    Foo<String> { "" }
}
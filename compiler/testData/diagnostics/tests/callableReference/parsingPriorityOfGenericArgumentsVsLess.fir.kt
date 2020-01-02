package test

class Foo {
    fun <T> bar(x: Int) = x
}

fun test() {
    Foo::bar <!SYNTAX!>< Int ><!> <!SYNTAX!>(2 + 2)<!>
}

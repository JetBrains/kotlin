package test

class Foo {
    fun <T> bar(x: Int) = x
}

fun test() {
    Foo::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> <!SYNTAX!>< Int ><!> <!SYNTAX!>(2 + 2)<!>
}

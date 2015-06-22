package test

class Foo {
}

class Owner {
    fun foo() {

    }

    fun Foo./*rename*/bar() {
    }

    fun Foo.bar(a: Int) {

    }

    fun Any.foo() {

    }
}
package test

class Foo {
}

class Owner {
    fun foo() {

    }

    fun Foo./*rename*/foo() {
    }

    fun Foo.foo(a: Int) {

    }

    fun Any.foo() {

    }
}
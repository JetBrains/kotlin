package test

class Foo

fun Foo.one() {}

fun Foo.usage() {
    fun one() {}

    <expr>this.one()</expr>
}

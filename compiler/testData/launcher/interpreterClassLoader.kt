package test

// for more information see KT-54509
object Test {
    fun foo(): String = "foo " + this

    fun bar(): String = "bar $this"

    fun baz(): String = "baz " + this.toString()
}

fun main() {
    println(Test.foo() + Test.bar() + Test.baz())
}

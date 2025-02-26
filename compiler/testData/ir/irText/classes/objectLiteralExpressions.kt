// FIR_IDENTICAL
// WITH_STDLIB

interface IFoo {
    fun foo()
}

val test1 = object {}

val test2 = object : IFoo {
    override fun foo() {
        println("foo")
    }
}

class Outer {
    abstract inner class Inner : IFoo

    fun test3() = object : Inner() {
        override fun foo() {
            println("foo")
        }
    }
}

fun Outer.test4() =
        object : Outer.Inner() {
            override fun foo() {
                println("foo")
            }
        }

// FIR_IDENTICAL
// WITH_STDLIB
// DUMP_LOCAL_DECLARATION_SIGNATURES

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57430

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

// FIR_IDENTICAL
// ISSUE: KT-65789
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS

package bar

fun <T> take(arg: T): T = arg

class Owner {
    companion object {

        fun Foo(string: String?) {}

        class Foo(val str: String)

        fun Bar(string: String) {}

        class Bar(val str: String?)

        val foo = take<Unit>(Foo("1"))
        val bar = take<Unit>(Bar("2"))
    }

    val foo = take<Owner.Companion.Foo>(Foo("3"))
    val bar = take<Owner.Companion.Bar>(Bar("4"))
}

val ownerFoo = take<Unit>(Owner.Foo("1"))
val ownerCompanionFoo = take<Owner.Companion.Foo>(Owner.Companion.Foo("2"))

val ownerBar = take<Unit>(Owner.Bar("3"))
val ownerCompanionBar = take<Owner.Companion.Bar>(Owner.Companion.Bar("4"))

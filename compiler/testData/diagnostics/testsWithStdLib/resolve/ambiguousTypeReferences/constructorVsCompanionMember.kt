// FIR_IDENTICAL
// ISSUE: KT-65789
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
// FIR_DUMP

package bar

class Owner {
    companion object {

        fun Foo(string: String?) {}

        class Foo(val str: String)

        fun Bar(string: String) {}

        class Bar(val str: String?)

        val foo = Foo("1") // K1/K2: function
        val bar = Bar("2") // K1/K2: function
    }

    val foo = Foo("3") // K1/K2: constructor
    val bar = Bar("4") // K1/K2: constructor
}

val ownerFoo = Owner.Foo("1") // K1/K2: function
val ownerCompanionFoo = Owner.Companion.Foo("2") // K1/K2: constructor

val ownerBar = Owner.Bar("3") // K1/K2: function
val ownerCompanionBar = Owner.Companion.Bar("4") // K1/K2: constructor

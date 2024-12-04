// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

class Foo {
    companion object {
        fun bar() {}
        val baz = 42
    }
}

val x1 = <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>Foo::bar<!>
val x2 = Foo.Companion::bar
val x3 = <!INCORRECT_CALLABLE_REFERENCE_RESOLUTION_FOR_COMPANION_LHS!>Foo::baz<!>
val x4 = Foo.Companion::baz

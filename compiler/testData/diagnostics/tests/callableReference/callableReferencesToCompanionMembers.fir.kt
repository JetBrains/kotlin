// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

class Foo {
    companion object {
        fun bar() {}
        val baz = 42
    }
}

val x1 = Foo::bar
val x2 = Foo.Companion::bar
val x3 = Foo::baz
val x4 = Foo.Companion::baz
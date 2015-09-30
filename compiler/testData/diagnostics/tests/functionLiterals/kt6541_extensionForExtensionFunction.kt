// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo
fun (Foo.() -> Unit).invoke(b : Foo.() -> Unit)  {}

object Z {
    infix fun add(b : Foo.() -> Unit) : Z = Z
}

val t2 = Z add <!TYPE_MISMATCH!>{ } <!TOO_MANY_ARGUMENTS!>{ }<!><!>

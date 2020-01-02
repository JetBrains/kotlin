// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Foo
fun (Foo.() -> Unit).invoke(b : Foo.() -> Unit)  {}

object Z {
    <!INAPPLICABLE_INFIX_MODIFIER!>infix fun add(b : Foo.() -> Unit) : Z = Z<!>
}

val t2 = Z <!INAPPLICABLE_CANDIDATE!>add<!> { } { }

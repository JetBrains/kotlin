// ISSUE: KT-61182

object Foo

operator fun Foo.invoke(f: () -> Unit) {
    f()
}

fun test(g: () -> Int) {
    Foo(<!ARGUMENT_TYPE_MISMATCH!>g<!>)
}

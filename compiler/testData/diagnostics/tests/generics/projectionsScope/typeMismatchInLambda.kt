// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

class Out<out T> {
    fun id() = this
    fun foobar(x: Any) {}
}

class A<E> {
    inline fun foo(block: () -> E) {}
    inline fun bar(block: () -> Out<E>) {}
}

fun test(a: A<out CharSequence>, z: Out<CharSequence>) {
    a.foo {
        val x: String = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!> // Should be no TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS
        <!TYPE_MISMATCH!>""<!>
    }
    a.bar { <!TYPE_MISMATCH!>Out<CharSequence>()<!> }
    a.bar { Out() }
    a.bar { <!TYPE_MISMATCH!>z.id()<!> }

    a.foo {
        z.foobar(if (1 > 2) return@foo <!TYPE_MISMATCH!>""<!> else "")
        <!TYPE_MISMATCH!>""<!>
    }
}

// ISSUE: KT-55179
// SKIP_TXT

private class Foo {
    companion object {
        fun buildFoo() = Foo()
    }
}

internal <!NOTHING_TO_INLINE!>inline<!> fun foo() {
    <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>Foo<!>()
    Foo.Companion
    Foo.<!PRIVATE_CLASS_MEMBER_FROM_INLINE!>buildFoo<!>()
}


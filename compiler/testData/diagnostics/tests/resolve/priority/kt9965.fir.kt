// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

enum class Foo {
    FOO;

    companion object {
        fun valueOf(something: String) = 2

        fun values() = 1
    }
}

fun test() {
    Foo.<!AMBIGUITY!>values<!>() <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Array<Foo>>() }
    Foo.Companion.values() checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }

    Foo.<!AMBIGUITY!>valueOf<!>("") <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Foo>() }
    Foo.Companion.valueOf("") checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
}
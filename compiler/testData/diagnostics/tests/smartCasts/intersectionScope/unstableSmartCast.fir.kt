// !WITH_NEW_INFERENCE
// !CHECK_TYPE

interface A {
    fun foo(): CharSequence?
    fun baz(x: Any) {}
}

interface B {
    fun foo(): String
    fun baz(x: Int): String =""
    fun baz(x: Int, y: Int) {}

    fun foobar(): CharSequence?
}

interface C {
    fun foo(): String
    fun baz(x: Int): String =""
    fun baz(x: Int, y: Int) {}

    fun foobar(): String
}

var x: A = null!!

fun test() {
    x.foo().checkType { _<CharSequence?>() }

    if (x is B && x is C) {
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><CharSequence?>() }
        x.baz("")
        x.<!AMBIGUITY!>baz<!>(1).<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Unit>() }
        x.<!AMBIGUITY!>baz<!>(1, 2)

        x.<!AMBIGUITY!>foobar<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    }
}

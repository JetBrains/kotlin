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
        x.foo().checkType { _<CharSequence?>() }
        x.baz("")
        x.baz(1).checkType { _<Unit>() }
        x.baz(1, <!TOO_MANY_ARGUMENTS!>2<!>)

        x.<!UNRESOLVED_REFERENCE!>foobar<!>().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    }
}

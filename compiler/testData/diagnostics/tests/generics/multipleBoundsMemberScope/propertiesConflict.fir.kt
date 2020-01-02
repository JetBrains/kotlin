// !CHECK_TYPE

interface A {
    val foo: Any?
}

interface C: A {
    override val foo: String
}
interface B: A {
    override var foo: String?
}

fun <T> test(a: T) where T : B, T : C {
    a.<!AMBIGUITY!>foo<!> = ""
    a.<!AMBIGUITY!>foo<!> = null

    a.<!AMBIGUITY!>foo<!>.<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
}

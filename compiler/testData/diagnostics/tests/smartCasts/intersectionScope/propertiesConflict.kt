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

fun test(a: A) {
    if (a is B && a is C) {
        a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> = ""
        a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> = null
        a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
    }
}

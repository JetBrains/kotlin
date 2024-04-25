// CHECK_TYPE

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
    a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> = ""
    a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> = null

    a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
}

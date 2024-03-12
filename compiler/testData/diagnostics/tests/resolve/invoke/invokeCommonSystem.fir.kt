// ISSUE: KT-58259

interface Box<T> {
    val value: T
}

interface Res {
    operator fun invoke() {}
}

val <X> Box<X>.foo: X get() = TODO()

fun foo(p: Box<Res>) {
    p.value.invoke() // OK
    p.value() // OK

    p.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>foo<!>.invoke() // OK
    // Error in K1, OK in K2
    p.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>foo<!>()
}

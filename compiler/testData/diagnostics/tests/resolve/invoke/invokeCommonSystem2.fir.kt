// ISSUE: KT-58260

interface Box<T>

interface Res<E>

operator fun <F> Res<F>.invoke(f: F): F = TODO()

val <X> Box<in X>.foo: Res<X> get() = TODO()

fun foo(p: Box<in Any?>) {
    p.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS, TYPE_ARGUMENTS_NOT_ALLOWED!>foo<!>("").<!UNRESOLVED_REFERENCE!>length<!>
    p.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>foo<!>.invoke("").<!UNRESOLVED_REFERENCE!>length<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>p.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS, TYPE_ARGUMENTS_NOT_ALLOWED!>foo<!>("")<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>p.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>foo<!>.invoke("")<!>
}

// LANGUAGE: +NestedTypeAliases

class A<T>

class C<T> {
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias TA = A<T>
}

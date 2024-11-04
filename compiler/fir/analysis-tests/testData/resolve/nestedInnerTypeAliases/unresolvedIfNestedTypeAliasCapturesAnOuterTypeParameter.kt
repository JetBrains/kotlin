// LANGUAGE: +NestedTypeAliases

class A<T>

class B<T> {
    typealias NestedTA = A<T> // T should be UNRESOLVED
    inner typealias InnerTA = A<T> // OK
}
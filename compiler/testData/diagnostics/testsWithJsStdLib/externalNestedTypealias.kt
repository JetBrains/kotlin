// LANGUAGE: +NestedTypeAliases
external class A
class B

external interface I {
    typealias Foo = A
    typealias Bar = B
    typealias Baz = Int
}

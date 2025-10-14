// FIR_IDENTICAL
// LANGUAGE: +NestedTypeAliases
@file:JsModule("lib")

external class A
typealias Foo = A
typealias Bar = Int
typealias Baz = Foo

external interface I {
    typealias Foo = A
    typealias Bar = Int
    typealias Baz = Foo
}

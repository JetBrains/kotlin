// LANGUAGE: +NestedTypeAliases
external class A
class B

external interface I {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Foo = A<!>
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Bar = B<!>
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Baz = Int<!>
}

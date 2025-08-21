// LANGUAGE: +NestedTypeAliases
@file:JsModule("lib")

external class A
<!NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE!>typealias Foo = A<!>
<!NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE!>typealias Bar = Int<!>
<!NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE!>typealias Baz = Foo<!>

external interface I {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Foo = A<!>
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Bar = Int<!>
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Baz = Foo<!>
}

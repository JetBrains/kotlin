// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +NestedTypeAliases
@JsExport
class A

class B

@JsExport
interface I {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Foo = A<!>
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Bar = B<!>
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Baz = Int<!>
}

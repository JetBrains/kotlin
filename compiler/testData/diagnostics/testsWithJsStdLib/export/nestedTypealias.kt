// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +NestedTypeAliases
@JsExport
class A

class B

@JsExport
interface I {
    typealias Foo = A
    typealias Bar = <!NON_EXPORTABLE_TYPE!>B<!>
    typealias Baz = Int
}

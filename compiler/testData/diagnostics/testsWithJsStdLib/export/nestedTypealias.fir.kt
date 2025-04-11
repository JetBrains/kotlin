// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +NestedTypeAliases
@JsExport
class A

class B

@JsExport
interface I {
    typealias Foo = A
    typealias Bar = B
    typealias Baz = Int
}

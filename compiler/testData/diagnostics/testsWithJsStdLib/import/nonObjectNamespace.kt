// FIR_IDENTICAL
package foo

<!JS_IMPORT_NAMESPACE_ON_NON_OBJECT_DECLARATION!>@JsImport("A")
<!WRONG_ANNOTATION_TARGET!>@JsImport.Namespace<!>
external fun foo()<!>

@JsImport("A")
@JsImport.Namespace
external class <!JS_IMPORT_NAMESPACE_ON_NON_OBJECT_DECLARATION!>Bar<!>

<!JS_IMPORT_NAMESPACE_ON_NON_OBJECT_DECLARATION!>@JsImport("A")
<!WRONG_ANNOTATION_TARGET!>@JsImport.Namespace<!>
external val test: String<!>

@JsImport("A")
@JsImport.Namespace
external interface <!JS_IMPORT_NAMESPACE_ON_NON_OBJECT_DECLARATION!>Foo<!>

@JsImport("A")
@JsImport.Namespace
external object Correct

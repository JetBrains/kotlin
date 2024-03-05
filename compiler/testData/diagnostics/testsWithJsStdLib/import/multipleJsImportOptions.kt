// FIR_IDENTICAL
package foo

<!JS_IMPORT_DEFAULT_AND_NAMED!>@JsImport("A")
@JsImport.Default
@JsImport.Name("Foo")
external fun foo(): String<!>

<!JS_IMPORT_DEFAULT_AND_NAMED, JS_IMPORT_NAMESPACE_ON_NON_OBJECT_DECLARATION!>@JsImport("A")
@JsImport.Default
<!WRONG_ANNOTATION_TARGET!>@JsImport.Namespace<!>
external fun bar(): String<!>

<!JS_IMPORT_DEFAULT_AND_NAMED, JS_IMPORT_NAMESPACE_ON_NON_OBJECT_DECLARATION!>@JsImport("A")
<!WRONG_ANNOTATION_TARGET!>@JsImport.Namespace<!>
@JsImport.Name("Foo")
external fun baz(): String<!>

<!JS_IMPORT_DEFAULT_AND_NAMED, JS_IMPORT_NAMESPACE_ON_NON_OBJECT_DECLARATION!>@JsImport("A")
@JsImport.Default
<!WRONG_ANNOTATION_TARGET!>@JsImport.Namespace<!>
@JsImport.Name("Foo")
external fun boo(): String<!>

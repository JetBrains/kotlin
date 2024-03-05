// FIR_IDENTICAL
package foo

<!JS_IMPORT_OPTION_WITHOUT_JS_IMPORT!>@JsImport.Name("Foo")
external fun foo()<!>

@JsImport.Default
external class <!JS_IMPORT_OPTION_WITHOUT_JS_IMPORT!>Bar<!>

@JsImport.Namespace
external <!JS_IMPORT_OPTION_WITHOUT_JS_IMPORT!>object Correct<!>

// FIR_IDENTICAL
package foo

<!JS_IMPORT_AND_JS_MODULE_MIX!>@JsImport("A")
@JsModule("A")
@JsImport.Default
external fun foo(): String<!>

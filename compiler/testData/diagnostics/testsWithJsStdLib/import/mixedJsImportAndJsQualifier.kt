// FIR_IDENTICAL
@file:JsQualifier("a.b.c")
package foo

<!JS_IMPORT_AND_JS_MODULE_MIX!>@JsImport("A")
@JsImport.Default
external fun foo(): String<!>

// FIR_IDENTICAL
package foo

<!JS_IMPORT_PROHIBITED_ON_VAR!>@JsImport("bar")
external var bar: Int<!> = definedExternally

typealias JsI = JsImport

<!JS_IMPORT_PROHIBITED_ON_VAR!>@JsI("bar")
external var bar2: Int<!> = definedExternally

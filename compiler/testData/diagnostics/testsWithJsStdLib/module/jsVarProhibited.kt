package foo

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsModule("bar")
external var bar: Int<!> = definedExternally

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsNonModule
external var baz: Int<!> = definedExternally
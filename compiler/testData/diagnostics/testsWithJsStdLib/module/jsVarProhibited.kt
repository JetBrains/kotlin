package foo

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsModule("bar")
@native var bar: Int<!> = noImpl

<!JS_MODULE_PROHIBITED_ON_VAR!>@JsNonModule
@native var baz: Int<!> = noImpl
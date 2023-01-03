package foo

@JsModule("bar")
external var bar: Int = definedExternally

@JsNonModule
external var baz: Int = definedExternally

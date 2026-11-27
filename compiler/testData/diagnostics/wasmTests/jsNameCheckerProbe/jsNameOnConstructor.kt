// RUN_PIPELINE_TILL: CODEGEN
// Primary constructor: should report JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED,
// like K/JS (testsWithJsStdLib/name/jsNamePrihibitedOnPrimaryConstructor.kt).
// Secondary constructor: on external classes, @JsName is silently ignored, `new B(...)` is always called.
package foo

// Primary constructor
external class A @JsName("A_") constructor()

// Secondary constructors
class B(x: Int) {
    @JsName("fromString") constructor(s: String) : this(s.length)
}

external class C(x: Int) {
    @JsName("fromString") constructor(s: String)
}

external class D {
    @JsName("fromInt") constructor(x: Int)
    @JsName("fromString") constructor(s: String)
}

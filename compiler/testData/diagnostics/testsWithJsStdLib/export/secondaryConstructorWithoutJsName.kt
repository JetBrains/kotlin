// !USE_EXPERIMENTAL: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES

package foo

@JsExport
class C(val x: String) {
    <!WRONG_EXPORTED_DECLARATION("secondary constructor without @JsName")!>constructor(x: Int)<!>: this(x.toString())
}

@JsExport
class C2(val x: String) {
    @JsName("JsNameProvided")
    constructor(x: Int): this(x.toString())
}
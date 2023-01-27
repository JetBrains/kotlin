// FIR_IDENTICAL
// !OPT_IN: kotlin.js.ExperimentalJsExport
// !RENDER_DIAGNOSTICS_MESSAGES
@file:JsExport

package foo

class C(val x: String) {
    <!WRONG_EXPORTED_DECLARATION("secondary constructor without @JsName")!>constructor(x: Int)<!>: this(x.toString())
}

class C2(val x: String) {
    @JsName("JsNameProvided")
    constructor(x: Int): this(x.toString())
}

class C3(val x: String) {
    protected <!WRONG_EXPORTED_DECLARATION("secondary constructor without @JsName")!>constructor(x: Int)<!>: this(x.toString())
}

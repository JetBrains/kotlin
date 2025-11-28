// RUN_PIPELINE_TILL: BACKEND
// Diagnostic should be added when KT-82785 is fixed
external class C {
    @JsQualifier("a")
    fun o(): String

    @JsQualifier("b")
    class D {
        fun k(): String
    }
}

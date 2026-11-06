// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82785
external class C {
    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("a")<!>
    fun o(): String

    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL!>@JsQualifier("b")<!>
    class D {
        fun k(): String
    }
}

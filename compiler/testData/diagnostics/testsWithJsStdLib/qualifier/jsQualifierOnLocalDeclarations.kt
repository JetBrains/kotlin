// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProhibitJsQualifierOnMembersAndNonExternals
// ISSUE: KT-82785
external class C {
    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL_ERROR!>@JsQualifier("a")<!>
    fun o(): String

    <!JS_QUALIFIER_PROHIBITED_ON_NON_TOP_LEVEL_ERROR!>@JsQualifier("b")<!>
    class D {
        fun k(): String
    }
}

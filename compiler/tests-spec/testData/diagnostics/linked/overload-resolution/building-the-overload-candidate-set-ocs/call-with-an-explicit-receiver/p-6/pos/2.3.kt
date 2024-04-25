// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 2
 * PRIMARY LINKS: overload-resolution, c-level-partition -> paragraph 1 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 3 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 2 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, call-with-trailing-lambda-expressions -> paragraph 1 -> sentence 2
 * NUMBER: 3
 * DESCRIPTION: set of non-extension member callables only
 */
// TESTCASE NUMBER: 1
class Case1 {
    operator fun String.invoke(): String {
        return this
    }

    operator fun String?.invoke(x: Any? = null, body: () -> String = { "" }): String = body()

    fun String.foo(): String = this
    fun bar() {
        "".<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: extension function")!>foo()<!>
        "".<!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>invoke()<!>
        <!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>""()<!>
        <!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>""()()()()  ()<!>
        <!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>""().invoke()()()<!>
        <!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>""().foo()().invoke()()<!>
        <!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>"1".foo()()<!>
        <!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>"1"()().invoke()()<!>.<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: extension function")!>foo()<!>
    }

    fun testTrailingLambda(s: String?){
        //trailing lambda
        s.<!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>invoke { "ss" }<!>
        s.<!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>invoke (x= 1) { "ss" }<!>
        s.<!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>invoke (body = { "ss" })<!>
        s.<!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: operator extension function")!>invoke (body = { "ss" }, x = '1')<!>

        <!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: variable&invoke")!>s (x= 1) { "ss" }<!>
        <!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: variable&invoke")!>s (body = { "ss" })<!>
        <!DEBUG_INFO_CALL("fqName: Case1.invoke; typeCall: variable&invoke")!>s (body = { "ss" }, x = '1')<!>

    }
}
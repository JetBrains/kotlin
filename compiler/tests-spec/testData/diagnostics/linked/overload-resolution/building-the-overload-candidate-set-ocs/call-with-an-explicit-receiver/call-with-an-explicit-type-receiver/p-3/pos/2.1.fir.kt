// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39129
 */
fun case1() {
    C1.<!INAPPLICABLE_CANDIDATE!>V<!>()

    C1.Companion.<!DEBUG_INFO_CALL("fqName: C1.Companion.V.V; typeCall: function")!>V()<!>

}

class C1(){
    companion object {
        class V(){
        }
    }
}


/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39129
 */
fun case2() {
    C2.<!DEBUG_INFO_CALL("fqName: L.invoke; typeCall: variable&invoke")!>V()<!> // to (2)
    C2.Companion.<!DEBUG_INFO_CALL("fqName: C2.Companion.V.V; typeCall: function")!>V()<!> // to (1)
}

open class C2(){
    companion object  {
        class V //(1)
    }

    object V : L()

}

open class L {
    operator fun invoke() {} //(2)
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39129
 */
fun case3() {
    C3.<!DEBUG_INFO_CALL("fqName: L3.invoke; typeCall: variable&invoke")!>V()<!> // to (2)
    C3.Companion.<!DEBUG_INFO_CALL("fqName: C3.Companion.V.V; typeCall: function")!>V()<!> // to (1)
}

open class C3(){
    companion object  {
        class V //(1)
    }

    object V : L3()

}

open class L3 {
    operator fun invoke(s : String ="") {} //(2)
}

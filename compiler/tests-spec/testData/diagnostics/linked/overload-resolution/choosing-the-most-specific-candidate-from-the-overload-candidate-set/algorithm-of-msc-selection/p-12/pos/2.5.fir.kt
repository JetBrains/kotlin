// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1{
    infix fun boo(x: Int): String = TODO() //(1.1)
    infix fun boo(x: Long): Unit = TODO() //(1.2)
    infix fun boo(x: Short): Unit = TODO() //(1.3)
    infix fun boo(x: Byte): Unit = TODO() //(1.4)
}

fun case1(case: Case1) {
    case.apply {
        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: infix function")!>this boo 1<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>this boo 1<!>

        //to (1.1)
        <!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: infix function")!>boo(1)<!>
        //(1.1) return type is String
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(1)<!>
    }
    //to (1.1)
    <!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: infix function")!>case boo 1<!>
    //(1.1) return type is String
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case boo 1<!>

    //to (1.1)
    case.<!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: infix function")!>boo(1)<!>
    //(1.1) return type is String
    case.boo(1)
}

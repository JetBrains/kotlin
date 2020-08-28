// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
class A : B, C
interface B
interface C
fun <T>foo(x: B) {} //(1)
fun foo(y: C, z: String = "foo") : String = "" //2
fun case1(a: A) {
    <!DEBUG_INFO_CALL("fqName: foo; typeCall: function")!>foo(a)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(a)<!>
}

// TESTCASE NUMBER: 2
class A2 : B2, C2
interface B2
interface C2
fun boo(x: B2) ="" //(1)
fun <T>boo(y: C, z: String = "boo") {} //2
fun case2(a: A2) {
    <!DEBUG_INFO_CALL("fqName: boo; typeCall: function")!>boo(a)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>boo(a)<!>
}

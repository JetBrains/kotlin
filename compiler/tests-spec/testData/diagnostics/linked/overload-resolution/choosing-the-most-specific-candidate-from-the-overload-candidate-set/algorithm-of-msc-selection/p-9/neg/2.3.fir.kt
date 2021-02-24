// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
interface A1
interface B1

fun case1(x: Any) {
    fun A1.foo(): kotlin.Int = 1  //(1)
    fun B1.foo(): kotlin.Int = 2  //(2)
    fun Any.foo(): kotlin.String = "Any"  //(3)
    x.<!DEBUG_INFO_CALL("fqName: case1.foo; typeCall: extension function")!>foo()<!> // to (3)
    x.foo()
    if (x is B1 && x is A1) {
        x.<!AMBIGUITY!>foo<!>()
    }
}

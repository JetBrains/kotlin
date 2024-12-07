// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-312
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, call-with-specified-type-parameters -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION:  filtering is done before selection of the overload candidate set is perfomed
 */
// TESTCASE NUMBER: 1

class Case1 {
    infix fun <T> foo(a: T): T = TODO() //(1)


    fun case1() {

        fun <T> foo(a: T): T = TODO() //(2)

        <!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: infix function")!>this foo "1"<!> // to (1)
        this.<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: infix function")!>foo<String>("")<!> // to (1)
        this.<!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: infix function")!>foo("")<!> // to (1)

        <!DEBUG_INFO_CALL("fqName: Case1.case1.foo; typeCall: function")!>foo("")<!> // to (2)
    }
}

// TESTCASE NUMBER: 2

fun <T> foo(): T = TODO() //(3)
fun case2() {

    fun <T, R> foo(): T = TODO() //(4)
    fun foo(): Unit = TODO() // (5)

    <!DEBUG_INFO_CALL("fqName: foo; typeCall: function")!>foo<String>()<!>  // to (3)
    <!DEBUG_INFO_CALL("fqName: case2.foo; typeCall: function")!>foo<String, String>()<!>  // to (4)

    <!DEBUG_INFO_CALL("fqName: case2.foo; typeCall: function")!>foo()<!> // to (5)
}
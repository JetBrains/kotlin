// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

// TESTCASE NUMBER: 1, 2
class ClassWithEqualsOverride {
    override fun equals(other: Any?) = true
    fun fun_1() = true
}

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28239
 */
fun case_1() {
    val x: ClassWithEqualsOverride? = null
    val y = ClassWithEqualsOverride()
    if (y == x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithEqualsOverride? & ClassWithEqualsOverride")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithEqualsOverride? & ClassWithEqualsOverride")!>x<!>.fun_1()
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28239
 */
fun case_2() {
    val x: ClassWithEqualsOverride? = null
    val y: ClassWithEqualsOverride? = ClassWithEqualsOverride()
    if (y != null) {
        if (y == x) {
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithEqualsOverride? & ClassWithEqualsOverride")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithEqualsOverride? & ClassWithEqualsOverride")!>x<!>.fun_1()
        }
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28239
 */
fun case_3() {
    val x: ClassWithEqualsOverride? = null
    val y: ClassWithEqualsOverride? = ClassWithEqualsOverride()
    if (y!! == x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithEqualsOverride? & ClassWithEqualsOverride")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithEqualsOverride? & ClassWithEqualsOverride")!>x<!>.fun_1()
    }
}

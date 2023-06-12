// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 40
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-28242
 */
fun case_1(x: Any?) {
    if (x is Int?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x!!<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-28242
 */
fun case_2(x: Any?) {
    if (x is Int?) {
        (x as Int).inv()
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?) {
    if (x is Int?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x!!<!>.run {
            inv()
        }
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?) {
    if (x is Int?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x!!<!>)<!>.inv()
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Any?) {
    x!!
    if (x is Int?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.inv()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>select(x)<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 */
fun case_6(x: Any?) {
    if (x is Boolean? && x!!) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Boolean")!>x<!>.not()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>select(x)<!>.not()
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: Boolean?) {
    if (x != null && x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean")!>x<!>.not()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>select(x)<!>.not()
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: Any?) {
    if (x != null && x is Boolean?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Boolean")!>x<!>.not()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>select(x)<!>.not()
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: Any?) {
    if (x is Boolean? && x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Boolean")!>x<!>.not()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>select(x)<!>.not()
    }
}

// TESTCASE NUMBER: 10
fun case_10(x: Any?) {
    if (x as Boolean) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Boolean")!>x<!>.not()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>select(x)<!>.not()
    }
}

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_11(x: Any?) {
    if ((x as Boolean?)!!) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Boolean")!>x<!>.not()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>select(x)<!>.not()
    }
}

/*
 * TESTCASE NUMBER: 12
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_12(x: Any?) {
    if (<!CONDITION_TYPE_MISMATCH!>x!! as Boolean?<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Boolean")!>x<!>.not()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>select(x)<!>.not()
    }
}

/*
 * TESTCASE NUMBER: 13
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30376
 */
fun case_13(x: Any?) {
    if (x as Boolean? ?: x!!) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Boolean")!>x<!>.not()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean")!>select(x)<!>.not()
    }
}

/*
 * TESTCASE NUMBER: 14
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28430
 */
fun case_14(x: Any?) {
    if (x == null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>?.equals(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x!!<!>.equals(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing")!>x<!>.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 15
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28430
 */
fun case_15(x: Any?) {
    if (x !== null) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>?.equals(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x!!<!>.equals(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing")!>x<!>.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 16
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28430
 */
fun case_16(x: Nothing?) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>?.<!NONE_APPLICABLE!>equals<!>(10)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>x!!<!>.<!NONE_APPLICABLE!>equals<!>(10)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Nothing")!>x<!>.<!NONE_APPLICABLE!>equals<!>(10)
}

// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 22
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_1(x: Boolean?, y: Boolean?) {
    while (true) {
        y != null || break
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_2(x: Boolean?, y: Boolean?) {
    do {
        y != null || break
        x!!
    } while (true)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_3(x: Boolean?, y: Boolean?) {
    while (true) {
        y != null && break
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_4(x: Boolean?, y: Boolean?) {
    do {
        y != null && break
        x!!
    } while (true)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

// TESTCASE NUMBER: 5
fun case_5(x: Boolean?, y: Boolean?) {
    while (true) {
        y.<!UNREACHABLE_CODE!><!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>to<!>(<!>break<!UNREACHABLE_CODE!>)<!>
        <!UNREACHABLE_CODE!>x!!<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 6
fun case_6(x: Boolean?, y: ((x: Nothing) -> Unit)?) {
    while (true) {
        y!!<!UNREACHABLE_CODE!>(<!>break<!UNREACHABLE_CODE!>)<!>
        <!UNREACHABLE_CODE!>x!!<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 7
fun case_7(x: Boolean?, y: Boolean?) {
    while (true) {
        y ?: break
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_8(x: Boolean?, y: Boolean?) {
    while (true) {
        y ?: (y!! || break)
        x!!
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}

/*
 * TESTCASE NUMBER: 9
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_9(x: Int?) {
    val y: Int
    while (true) {
        <!UNREACHABLE_CODE!>y =<!> break as Int <!UNREACHABLE_CODE!>+ x!!<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_10(x: Int?) {
    val y: Int
    do {
        break as Int <!UNREACHABLE_CODE!>+ x as Int<!>
    } while (<!UNREACHABLE_CODE!>true<!>)

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}

/*
 * TESTCASE NUMBER: 11
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_11(x: Int?) {
    operator fun Nothing.invoke(x: Any) = x

    val y: Int
    while (true) {
        break<!UNREACHABLE_CODE!>(x!!)<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}

/*
 * TESTCASE NUMBER: 12
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_12(x: Int?) {
    operator fun Nothing.get(x: Int, y: Int) = x + y

    val y: Int
    while (true) {
        <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>break<!UNREACHABLE_CODE!>[x!!]<!><!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}

/*
 * TESTCASE NUMBER: 13
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_13(x: Int?) {
    operator fun Nothing.set(<!UNUSED_PARAMETER!>x<!>: Int, <!UNUSED_PARAMETER!>y<!>: Int) {}

    val y: Int
    while (true) {
        break<!UNREACHABLE_CODE!>[x!!] = 10<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}

/*
 * TESTCASE NUMBER: 14
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28333
 */
fun case_14(x: Int?) {
    operator fun Nothing.set(<!UNUSED_PARAMETER!>x<!>: Int, <!UNUSED_PARAMETER!>y<!>: Int) {}

    val y: Int
    while (true) {
        break<!UNREACHABLE_CODE!>[10] = x!!<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
}

// TESTCASE NUMBER: 15
fun case_15(x: Int?) {
    operator fun Nothing.invoke(x: () -> Unit) = x()

    val y: Int
    while (true) {
        break <!UNREACHABLE_CODE!>{
            x!!
        }<!>
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>.<!UNRESOLVED_REFERENCE!>not<!>()
}

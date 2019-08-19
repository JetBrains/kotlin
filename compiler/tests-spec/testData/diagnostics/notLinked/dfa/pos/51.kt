// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 51
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    val y = run {
        if (x is Class)
            return@run <!DEBUG_INFO_EXPRESSION_TYPE("Class & kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>
        Class()
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>.fun_1()
}

// TESTCASE NUMBER: 2
fun case_2(x: Class?) {
    val y = run {
        x!!
        return@run <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>.fun_1()
}

// TESTCASE NUMBER: 3
fun case_3(z: Any?) {
    val y = run {
        when (z) {
            is Class? -> <!DEBUG_INFO_SMARTCAST!>z<!>!!
            is Class -> return@run <!DEBUG_INFO_SMARTCAST!>z<!>
            is Float -> Class()
            else -> return@run Class()
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>.fun_1()
}

// TESTCASE NUMBER: 4
fun case_4(z: Any?) {
    val y = run {
        when (z) {
            is Class? -> <!DEBUG_INFO_SMARTCAST!>z<!>!!
            is Class -> return@run <!DEBUG_INFO_SMARTCAST!>z<!>
            is Float -> Class()
            else -> return@run Class()
        }
        Class()
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>.fun_1()
}

// TESTCASE NUMBER: 5
fun case_5(z: Any?) {
    val y = run {
        when (z) {
            is Class? -> <!DEBUG_INFO_SMARTCAST!>z<!>!!
            is Class -> return@run <!DEBUG_INFO_SMARTCAST!>z<!>
            is Float -> Class()
            else -> return@run Class()
        }
        ""
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 6
fun case_6(z: Any?) {
    val y = z.let {
        when (it) {
            is Class? -> <!DEBUG_INFO_SMARTCAST!>it<!>!!
            is Class -> return@let <!DEBUG_INFO_SMARTCAST!>it<!>
            is Float -> Class()
            else -> return@let Class()
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>.fun_1()
}

// TESTCASE NUMBER: 7
fun case_7(z: Any?) {
    val y = z.let {
        it as Class
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>.fun_1()
}

// TESTCASE NUMBER: 8
fun case_8(z: Any?) {
    val y = z.let {
        return@let it as Class
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>.fun_1()
}

// TESTCASE NUMBER: 9
fun case_9(z: Any?) {
    val y = z.run {
        this as Class
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>.fun_1()
}

// TESTCASE NUMBER: 10
fun case_10(z: Any?) {
    val y = z.run {
        return@run this as Class
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>.fun_1()
}

// TESTCASE NUMBER: 11
fun case_11(z: Any?, x: Any?) {
    val y = z.let {
        if (it is ClassLevel6)
            return@let <!DEBUG_INFO_SMARTCAST!>it<!>
        x as ClassLevel3
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel3")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel3")!>y<!>.test3()
}

// TESTCASE NUMBER: 12
fun case_12(z: Any?) {
    val y = z.let {
        return@let it as Int
        <!UNREACHABLE_CODE!>it as? Float ?: 10f<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Float & Int}> & Number}")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Float & Int}> & Number}")!>y<!>.toByte()
}

/*
 * TESTCASE NUMBER: 13
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30927
 */
fun case_13(z: Any?) {
    val y = z.run {
        if (this is ClassLevel6)
            return@run <!TYPE_MISMATCH!>this<!>
        this as ClassLevel3
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel3")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel3")!>y<!>.test3()
}

// TESTCASE NUMBER: 14
fun case_14(z: Any?) {
    val y = z.run {
        return@run this as Int
        <!UNREACHABLE_CODE!>this as? Float ?: 10f<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Float & Int}> & Number}")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("{Comparable<{Float & Int}> & Number}")!>y<!>.toByte()
}

/*
 * TESTCASE NUMBER: 15
 * NOTE: 'Any' is common super type between kotlin.Unit (obtained using coersion to Unit) and Int
 */
fun case_15(z: Any?) {
    val y = z.let {
        return@let it as Int
        <!UNREACHABLE_CODE!>while (true) { println(1) }<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 16
 * NOTE: 'Any' is common super type between kotlin.Unit (obtained using coersion to Unit) and Int
 */
fun case_16(z: Any?) {
    val y = z.run {
        return@run this as Int
        <!UNREACHABLE_CODE!>while (true) { println(1) }<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 17
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30927
 */
fun case_17(z: Any?) {
    val y = z.run {
        when (this) {
            is Class? -> <!DEBUG_INFO_SMARTCAST!>this<!>!!
            is Class -> return@run <!TYPE_MISMATCH!>this<!>
            is Float -> Class()
            else -> return@run Class()
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>y<!>.fun_1()
}

/*
 * TESTCASE NUMBER: 18
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30927
 */
fun case_18(z: Any?) {
    val y = z.run {
        this as Int
        <!TYPE_MISMATCH!>this<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>y<!>.inv()
}

// TESTCASE NUMBER: 19
fun case_19(z: Any?) {
    val y = z.let {
        it as Int
        <!DEBUG_INFO_SMARTCAST!>it<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>y<!>.inv()
}

/*
 * TESTCASE NUMBER: 20
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30927
 */
fun case_20(z: Any?) {
    val y = z.run {
        this!!
        <!TYPE_MISMATCH!>this<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 21
fun case_21(z: Any?) {
    val y = z.run {
        if (true) this as Any else this!!
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 22
fun case_22(z: Any?) {
    val y = z.let {
        if (true) it as Any else it!!
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 23
fun case_23(z: Any?) {
    val y = z.run {
        when (this) {
            true -> this<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
            0.0 -> this <!USELESS_CAST!>as Any<!>
            else -> this!!
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 24
fun case_24(z: Any?) {
    val y = z.let {
        when (it) {
            true -> it<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
            0.0 -> it <!USELESS_CAST!>as Any<!>
            else -> it!!
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 25
fun case_25(z: Any?) {
    val y = z.run {
        when (this) {
            true -> <!DEBUG_INFO_SMARTCAST!>this<!>
            if (true) this as Int else this as Float -> <!DEBUG_INFO_SMARTCAST!>this<!>
            return@run this as Float<!UNREACHABLE_CODE!><!> -> <!DEBUG_INFO_SMARTCAST, UNREACHABLE_CODE!>this<!>
            <!UNREACHABLE_CODE!>else -> this<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 26
fun case_26(z: Any?) {
    val y = z.let {
        when (it) {
            true -> <!DEBUG_INFO_SMARTCAST!>it<!>
            if (true) it as Int else it as Float -> <!DEBUG_INFO_SMARTCAST!>it<!>
            return@let it as Int<!UNREACHABLE_CODE!><!> -> <!DEBUG_INFO_SMARTCAST, UNREACHABLE_CODE!>it<!>
            <!UNREACHABLE_CODE!>else -> it<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 27
fun case_27(z: Any?) {
    val y = z.let {
        if (it == null) return@let Any()
        <!DEBUG_INFO_SMARTCAST!>it<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 28
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30927
 */
fun case_28(z: Any?) {
    val y = z.run {
        if (this == null) throw IllegalStateException()
        <!TYPE_MISMATCH!>this<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

// TESTCASE NUMBER: 29
fun case_29(z: Any?) {
    val y = z.let {
        if (it == null) throw IllegalStateException()
        <!DEBUG_INFO_SMARTCAST!>it<!>
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>y<!>.equals(10)
}

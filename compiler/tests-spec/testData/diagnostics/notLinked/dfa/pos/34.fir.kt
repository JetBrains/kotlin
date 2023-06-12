// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 34
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1() {
    var a: Any? = null
    if (a == null) return
    val b = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>)
    val c = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Any")!>c<!>.equals(10)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.equals(10)
}

// TESTCASE NUMBER: 2
fun case_2(a: Any?) {
    if (a is String) {
        val b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>b<!>.length
    }
}

// TESTCASE NUMBER: 3
fun case_3(a: Any?) {
    if (a is String) {
        val b = a
        val c = b
        val d = c
        val e = d
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>e<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>e<!>.length
    }
}

// TESTCASE NUMBER: 4
fun case_4(a: Any?) {
    if (a is ClassLevel1) {
        val b = a
        if (b is ClassLevel2) {
            val c = b
            if (c is ClassLevel3) {
                val d = c
                if (d is ClassLevel4) {
                    val e = d
                    if (e is ClassLevel5) {
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5")!>e<!>
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5")!>e<!>.test1()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5")!>e<!>.test2()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5")!>e<!>.test3()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5")!>e<!>.test4()
                        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5")!>e<!>.test5()
                    }
                }
            }
        }
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * DISCUSSION: Smartcast due to `e as? ClassLevel5 ?: e as ClassLevel5`
 * ISSUES: KT-30503
 */
fun case_5(a: Any?) {
    val b: Any?
    val c: Any?
    val d: Any?
    val e: Any?
    if (
        a is ClassLevel1
        && if (true) {b = a; false} else {b = a;true}
        && <!USELESS_IS_CHECK!><!UNINITIALIZED_VARIABLE!>b<!> as ClassLevel2 is ClassLevel2<!>
        && if (true) {c = <!UNINITIALIZED_VARIABLE!>b<!>;false} else {c = <!UNINITIALIZED_VARIABLE!>b<!>;false}
        && try {<!UNINITIALIZED_VARIABLE!>c<!> as ClassLevel3;true} finally {<!UNINITIALIZED_VARIABLE!>c<!> as ClassLevel3;false}
        && when (true) {else -> {d = <!UNINITIALIZED_VARIABLE!>c<!>;true}}
        && when (true) {else -> {<!UNINITIALIZED_VARIABLE!>d<!> as ClassLevel4;false}}
        && if (true) {e = <!UNINITIALIZED_VARIABLE!>d<!>;false} else {e = <!UNINITIALIZED_VARIABLE!>d<!>;true}
        && if (true) {<!UNINITIALIZED_VARIABLE!>e<!> as? ClassLevel5 ?: <!UNINITIALIZED_VARIABLE!>e<!> as ClassLevel5;true} else {<!UNINITIALIZED_VARIABLE!>e<!> as? ClassLevel5 ?: <!UNINITIALIZED_VARIABLE!>e<!> as ClassLevel5;false}
            ) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5"), UNINITIALIZED_VARIABLE!>e<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5"), UNINITIALIZED_VARIABLE!>e<!>.test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5"), UNINITIALIZED_VARIABLE!>e<!>.test2()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5"), UNINITIALIZED_VARIABLE!>e<!>.test3()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5"), UNINITIALIZED_VARIABLE!>e<!>.test4()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel5"), UNINITIALIZED_VARIABLE!>e<!>.test5()
    }
}

// TESTCASE NUMBER: 5
fun case_6() {
    val b: Any?
    val c: Any?
    val d: Any?
    val e: Any?

    when (if (true) {b = 11} else {b = 12}) {
        when (if (true) {b.inv(); c = b; c as ClassLevel1;} else {b.inv(); c = b; c as ClassLevel1;}) {
            else -> kotlin.Unit
        } -> when (if (true) {c.test1(); d = c; d as ClassLevel2} else {c.test1(); d = c; d as ClassLevel2}) {
            when (if (true) {d.test2(); e = d; e as ClassLevel3} else {d.test2(); e = d; e as ClassLevel3}) {
                else -> ClassLevel2()
            } -> {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int & ClassLevel3")!>e<!>
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int & ClassLevel3")!>e<!>.inv())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int & ClassLevel3")!>e<!>.test1())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int & ClassLevel3")!>e<!>.test2())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int & ClassLevel3")!>e<!>.test3())
            }
            else -> {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int & ClassLevel3")!>e<!>
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int & ClassLevel3")!>e<!>.inv())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int & ClassLevel3")!>e<!>.test1())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int & ClassLevel3")!>e<!>.test2())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int & ClassLevel3")!>e<!>.test3())
            }
        }
    }
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 */
fun case_7() {
    val d = ClassLevel1()
    var e: Any?

    e = d
    e as ClassLevel2
    e = d

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2")!>e<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2")!>e<!>.test1()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2")!>e<!>.test2()
}

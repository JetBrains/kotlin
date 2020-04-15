// !LANGUAGE: +NewInference
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
    val b = select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a<!>)
    val c = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>a<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any")!>c<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any")!>c<!>.equals(10)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>b<!>.equals(10)
}

// TESTCASE NUMBER: 2
fun case_2(a: Any?) {
    if (a is String) {
        val b = a
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String")!>b<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String")!>b<!>.length
    }
}

// TESTCASE NUMBER: 3
fun case_3(a: Any?) {
    if (a is String) {
        val b = a
        val c = b
        val d = c
        val e = d
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String")!>e<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String & kotlin.String")!>e<!>.length
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
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & ClassLevel4")!>e<!>
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & ClassLevel4")!>e<!>.test1()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & ClassLevel4")!>e<!>.test2()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & ClassLevel4")!>e<!>.test3()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & ClassLevel4")!>e<!>.test4()
                        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & ClassLevel4")!>e<!>.test5()
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
        && b as ClassLevel2 is ClassLevel2
        && if (true) {c = b;false} else {c = b;false}
        && try {c as ClassLevel3;true} finally {c as ClassLevel3;false}
        && when (true) {else -> {d = c;true}}
        && when (true) {else -> {d as ClassLevel4;false}}
        && if (true) {e = d;false} else {e = d;true}
        && if (true) {e as? ClassLevel5 ?: e as ClassLevel5;true} else {e as? ClassLevel5 ?: e as ClassLevel5;false}
            ) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?")!>e<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?")!>e<!>.test1()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?")!>e<!>.test2()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?")!>e<!>.test3()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?")!>e<!>.test4()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel5 & kotlin.Any?")!>e<!>.test5()
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
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & ClassLevel3 & kotlin.Any?")!>e<!>
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & ClassLevel3 & kotlin.Any?")!>e<!>.inv())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & ClassLevel3 & kotlin.Any?")!>e<!>.test1())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & ClassLevel3 & kotlin.Any?")!>e<!>.test2())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & ClassLevel3 & kotlin.Any?")!>e<!>.test3())
            }
            else -> {
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & ClassLevel3 & kotlin.Any?")!>e<!>
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & ClassLevel3 & kotlin.Any?")!>e<!>.inv())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & ClassLevel3 & kotlin.Any?")!>e<!>.test1())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & ClassLevel3 & kotlin.Any?")!>e<!>.test2())
                println(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & ClassLevel3 & kotlin.Any?")!>e<!>.test3())
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

    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?")!>e<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?")!>e<!>.test1()
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?")!>e<!>.test2()
}

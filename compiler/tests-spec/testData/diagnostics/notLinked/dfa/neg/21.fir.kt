// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28508
 */
class Case1 {
    val x: Int?
    init {
        val y = this
        if (y.x != null) {
            x = null
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int?")!>x<!>
            this.x
            y.x
            y.x.<!UNSAFE_CALL!>inv<!>()
        } else {
            x = 10
        }
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28508
 */
class Case2 {
    val x: Int
    init {
        val y = this
        if (y.x == null) {
            x = 11
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int")!>x<!>
            this.x
            y.x
            y.x.inv()
            println(1)
        } else {
            x = 11
        }
    }
}

// IGNORE_REVERSED_RESOLVE
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    val value_1: Int
    funWithAtLeastOnceCallsInPlace { <!VAL_REASSIGNMENT!>value_1<!> = 10 }
    value_1.inc()
}

// TESTCASE NUMBER: 2
fun case_2() {
    val value_1: Int
    funWithAtMostOnceCallsInPlace { value_1 = 10 }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 3
fun case_3() {
    val value_1: Int
    funWithUnknownCallsInPlace { <!VAL_REASSIGNMENT!>value_1<!> = 10 }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 4
fun case_4() {
    var value_1: Int
    var value_2: Int
    funWithAtMostOnceCallsInPlace { value_1 = 10 }
    funWithUnknownCallsInPlace { value_2 = 10 }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
    <!UNINITIALIZED_VARIABLE!>value_2<!>.div(10)
}

// TESTCASE NUMBER: 5
class case_5 {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val value_1: Int<!>
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val value_2: Int<!>
    val value_3: Int
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var value_4: Int<!>
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var value_5: Int<!>
    init {
        funWithAtMostOnceCallsInPlace { value_1 = 1 }
        funWithUnknownCallsInPlace { <!VAL_REASSIGNMENT!>value_2<!> = 1 }
        funWithAtLeastOnceCallsInPlace { <!VAL_REASSIGNMENT!>value_3<!> = 1 }
        funWithAtMostOnceCallsInPlace { value_4 = 2 }
        funWithUnknownCallsInPlace { value_5 = 3 }
    }
}

// TESTCASE NUMBER: 6
fun case_6() {
    val value_1: Int
    for (i in 0..1)
        funWithExactlyOnceCallsInPlace { <!VAL_REASSIGNMENT!>value_1<!> = 10 }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
}

// TESTCASE NUMBER: 7
fun case_7() {
    var value_1: Int
    var i = 0
    while (i < 10) {
        funWithExactlyOnceCallsInPlace { value_1 = 10 }
        i++
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
}

// TESTCASE NUMBER: 8
fun case_8() {
    var value_1: Int
    if (true) funWithAtLeastOnceCallsInPlace { value_1 = 10 }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
}

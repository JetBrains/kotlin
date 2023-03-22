// IGNORE_REVERSED_RESOLVE
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: EnumClass?) {
    val value_2: Int

    <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
        EnumClass.NORTH -> funWithExactlyOnceCallsInPlace { value_2 = 1 }
        EnumClass.SOUTH -> funWithExactlyOnceCallsInPlace { value_2 = 2 }
        EnumClass.EAST -> funWithExactlyOnceCallsInPlace { value_2 = 4 }
        null -> funWithExactlyOnceCallsInPlace { value_2 = 5 }
    }

    <!UNINITIALIZED_VARIABLE!>value_2<!>.inc()
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Any?) {
    val value_2: Int

    funWithAtMostOnceCallsInPlace {
        if (value_1 is String) {
            value_2 = 0
        } else if (value_1 == null) {
            value_2 = 1
        } else {
            funWithAtMostOnceCallsInPlace { value_2 = 2 }
        }
        <!UNINITIALIZED_VARIABLE!>value_2<!>.dec()
    }
    <!UNINITIALIZED_VARIABLE!>value_2<!>.dec()
}

// TESTCASE NUMBER: 3
class case_3(value_1: Any?) {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var value_2: Int<!>

    init {
        if (value_1 is String) {
            funWithUnknownCallsInPlace { value_2 = 0 }
            <!UNINITIALIZED_VARIABLE!>value_2<!>.div(10)
        } else if (value_1 == null) {
            funWithAtLeastOnceCallsInPlace { value_2 = 1 }
            value_2.div(10)
        } else {
            value_2 = 2
        }

        <!UNINITIALIZED_VARIABLE!>value_2<!>.div(10)
    }
}

// TESTCASE NUMBER: 4
fun case_4(value_1: EnumClassSingle?) {
    var value_2: Int

    funWithAtMostOnceCallsInPlace {
        when (value_1) {
            EnumClassSingle.EVERYTHING -> {
                funWithExactlyOnceCallsInPlace { value_2 = 1 }
                ++value_2
            }
            null -> {
                funWithUnknownCallsInPlace { value_2 = 2 }
            }
        }
        <!UNINITIALIZED_VARIABLE!>value_2<!>.minus(5)
    }
    <!UNINITIALIZED_VARIABLE!>value_2<!>.minus(5)
}

// TESTCASE NUMBER: 5
fun case_5() {
    var value_2: Int

    try {
        funWithAtLeastOnceCallsInPlace { value_2 = 10 }
    } catch (e: Exception) {
        funWithAtMostOnceCallsInPlace { value_2 = 1 }
    }

    <!UNINITIALIZED_VARIABLE!>value_2<!>++
}

// TESTCASE NUMBER: 6
fun case_6() {
    var value_2: Int

    try {
        funWithAtLeastOnceCallsInPlace { value_2 = 10 }
    } catch (e: Exception) {
        throw Exception()
    } finally {
        println(<!UNINITIALIZED_VARIABLE!>value_2<!>.inc())
    }

    value_2++
}

// TESTCASE NUMBER: 7
fun case_7() {
    var value_1: Int

    try {
        funWithAtLeastOnceCallsInPlace { value_1 = 10 }
    } catch (e: Exception) {
        try {
            funWithAtLeastOnceCallsInPlace { value_1 = 10 }
        } catch (e: Exception) {
            funWithAtMostOnceCallsInPlace { value_1 = 10 }
        }
    }

    println(<!UNINITIALIZED_VARIABLE!>value_1<!>.inc())
}

// TESTCASE NUMBER: 8
fun case_8() {
    val x: Int
    funWithExactlyOnceCallsInPlace outer@ {
        funWithAtMostOnceCallsInPlace {
            funWithUnknownCallsInPlace {
                <!VAL_REASSIGNMENT!>x<!> = 42
            }
            return@outer
        }
        throw Exception()
    }
    println(<!UNINITIALIZED_VARIABLE!>x<!>.inc())
}

// TESTCASE NUMBER: 9
fun case_9() {
    val x: Int
    funWithExactlyOnceCallsInPlace outer@ {
        funWithAtMostOnceCallsInPlace {
            x = 42
            return@outer
        }
    }
    println(<!UNINITIALIZED_VARIABLE!>x<!>.inc())
}

// TESTCASE NUMBER: 10
fun case_10() {
    var x: Int
    funWithAtLeastOnceCallsInPlace outer@ {
        funWithAtMostOnceCallsInPlace {
            x = 41
            return@outer
        }
        funWithUnknownCallsInPlace {
            x = 42
            return@outer
        }
        return@outer
    }
    println(<!UNINITIALIZED_VARIABLE!>x<!>.inc())
}

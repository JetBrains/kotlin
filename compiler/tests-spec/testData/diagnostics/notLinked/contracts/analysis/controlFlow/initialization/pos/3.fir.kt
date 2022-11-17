// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: EnumClass?) {
    val value_2: Int

    when (value_1) {
        EnumClass.NORTH -> funWithExactlyOnceCallsInPlace { value_2 = 1 }
        EnumClass.SOUTH -> funWithExactlyOnceCallsInPlace { value_2 = 2 }
        EnumClass.WEST -> funWithExactlyOnceCallsInPlace { value_2 = 3 }
        EnumClass.EAST -> funWithExactlyOnceCallsInPlace { value_2 = 4 }
        null -> funWithExactlyOnceCallsInPlace { value_2 = 5 }
    }

    value_2.inc()
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
            funWithExactlyOnceCallsInPlace { value_2 = 2 }
            value_2.dec()
        }
        value_2.dec()
    }
}

// TESTCASE NUMBER: 3
class case_3(value_1: Any?) {
    var value_2: Int

    init {
        if (value_1 is String) {
            funWithExactlyOnceCallsInPlace { value_2 = 0 }
            value_2.div(10)
        } else if (value_1 == null) {
            funWithAtLeastOnceCallsInPlace { value_2 = 1 }
            value_2.div(10)
        } else {
            value_2 = 2
        }

        value_2.div(10)
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
                funWithAtLeastOnceCallsInPlace { value_2 = 2 }
                --value_2
            }
        }
        value_2.minus(5)
    }
}

// TESTCASE NUMBER: 5
fun case_5() {
    var value_2: Int

    try {
        funWithAtLeastOnceCallsInPlace { value_2 = 10 }
    } catch (e: Exception) {
        funWithExactlyOnceCallsInPlace { value_2 = 1 }
    }

    value_2++
}

// TESTCASE NUMBER: 6
fun case_6() {
    var value_2: Int

    try {
        funWithAtLeastOnceCallsInPlace { value_2 = 10 }
    } catch (e: Exception) {
        throw Exception()
    } finally {
        funWithAtLeastOnceCallsInPlace { value_2 = 10 }
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
            throw Exception()
        }
    } finally {
        funWithAtLeastOnceCallsInPlace { value_1 = 10 }
    }

    println(value_1.inc())
}

// TESTCASE NUMBER: 8
fun case_8() {
    var value_1: Int

    try {
        funWithAtLeastOnceCallsInPlace { value_1 = 10 }
    } catch (e: Exception) {
        try {
            funWithAtLeastOnceCallsInPlace { value_1 = 10 }
        } catch (e: Exception) {
            funWithAtLeastOnceCallsInPlace { value_1 = 10 }
        }
    }

    println(value_1.inc())
}

// TESTCASE NUMBER: 9
fun case_9() {
    val x: Int
    funWithExactlyOnceCallsInPlace outer@ {
        funWithAtMostOnceCallsInPlace {
            funWithUnknownCallsInPlace {
                x = 42
                return@outer
            }
        }
        throw Exception()
    }
    println(x.inc())
}

// TESTCASE NUMBER: 10
fun case_10() {
    val x: Int
    funWithExactlyOnceCallsInPlace outer@ {
        funWithAtLeastOnceCallsInPlace {
            x = 42
            return@outer
        }
    }
    println(x.inc())
}

// TESTCASE NUMBER: 11
fun case_11() {
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
        return@case_11
    }
    println(x.inc())
}

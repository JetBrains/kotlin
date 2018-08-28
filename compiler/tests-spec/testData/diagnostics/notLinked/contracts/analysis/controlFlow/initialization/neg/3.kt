// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// !WITH_ENUM_CLASSES
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, initialization
 NUMBER: 3
 DESCRIPTION: val/var reassignment and/or uninitialized variable usages with compelx control flow inside/outside lambda of contract function with CallsInPlace effect
 */

fun case_1(value_1: _EnumClass?) {
    val value_2: Int

    <!NON_EXHAUSTIVE_WHEN!>when<!> (value_1) {
        _EnumClass.NORTH -> funWithExactlyOnceCallsInPlace { value_2 = 1 }
        _EnumClass.SOUTH -> funWithExactlyOnceCallsInPlace { value_2 = 2 }
        _EnumClass.EAST -> funWithExactlyOnceCallsInPlace { value_2 = 4 }
        null -> funWithExactlyOnceCallsInPlace { value_2 = 5 }
    }

    <!UNINITIALIZED_VARIABLE!>value_2<!>.inc()
}

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
    value_2.dec()
}

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

fun case_4(value_1: _EnumClassSingle?) {
    var value_2: Int

    funWithAtMostOnceCallsInPlace {
        when (value_1) {
            _EnumClassSingle.EVERYTHING -> {
                funWithExactlyOnceCallsInPlace { value_2 = 1 }
                ++value_2
            }
            null -> {
                funWithUnknownCallsInPlace { value_2 = 2 }
            }
        }
        <!UNINITIALIZED_VARIABLE!>value_2<!>.minus(5)
    }
    value_2.minus(5)
}

fun case_5() {
    var value_2: Int

    try {
        funWithAtLeastOnceCallsInPlace { value_2 = 10 }
    } catch (e: Exception) {
        funWithAtMostOnceCallsInPlace { value_2 = 1 }
    }

    <!UNINITIALIZED_VARIABLE!>value_2<!>++
}

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

fun case_8() {
    val x: Int
    funWithExactlyOnceCallsInPlace outer@ {
        funWithAtMostOnceCallsInPlace {
            funWithExactlyOnceCallsInPlace {
                x = 42
                return@outer
            }
        }
        throw Exception()
    }
    println(x.inc())
}

fun case_9() {
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

fun case_10() {
    val x: Int
    funWithExactlyOnceCallsInPlace outer@ {
        funWithAtMostOnceCallsInPlace {
            x = 42
            return@outer
        }
    }
    println(<!UNINITIALIZED_VARIABLE!>x<!>.inc())
}

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
        return@outer
    }
    println(<!UNINITIALIZED_VARIABLE!>x<!>.inc())
}

// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 5
 * NUMBER: 2
 * DESCRIPTION: 'When' with different variants of the arithmetic expressions (additive expression and multiplicative expression) in 'when condition'.
 * HELPERS: typesProvider, classes, functions
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Any?) {
    when (value_1) {
        true, 100, -.09f -> {}
        '.', "...", null -> {}
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Number, value_2: Int) {
    when (value_1) {
        -.09 % 10L, value_2 / -5, getByte() - 11 + 90 -> {}
    }
}

// TESTCASE NUMBER: 4
fun case_4(value_1: String, value_2: String, value_3: String) {
    when (value_1) {
        "..." + value_2 + "" + "$value_3" + "...", value_2 + getString() -> {}
    }
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Int, value_2: Int, value_3: Boolean?) {
    when (value_1) {
        when {
            value_2 > 1000 -> 1
            value_2 > 100 -> 2
            else -> 3
        }, when (value_3) {
            true -> 1
            false -> 2
            null -> 3
        }, when (value_3!!) {
            true -> 1
            false -> 2
        } -> {}
    }
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Int, value_2: Int) {
    when (value_1) {
        if (value_2 > 1000) 1 else 2, if (value_2 < 100) 1 else if (value_2 < 10) 2 else 3 -> {}
    }
}

// TESTCASE NUMBER: 7
fun case_7(value_1: Any, value_2: String, value_3: String) {
    when (value_1) {
        try { 4 } catch (e: Exception) { 5 }, try { throw Exception() } catch (e: Exception) { value_2 }, try { throw Exception() } catch (e: Exception) { {value_3} } finally { } -> {}
    }
}

// TESTCASE NUMBER: 8
fun case_8(value_1: Int, value_2: Int?, value_3: Int?) {
    when (value_1) {
        value_2 ?: 0, value_2 ?: value_3 ?: 0, value_2!! <!USELESS_ELVIS!>?: 0<!> -> {}
    }
}

// TESTCASE NUMBER: 9
fun case_9(value_1: Any) {
    when (value_1) {
        1..10, -100L..100L, -getInt()..getLong() -> {}
    }
}

// TESTCASE NUMBER: 10
fun case_10(value_1: Collection<Int>, value_2: Collection<Int>, value_3: Collection<Int>?) {
    when (value_1) {
        value_2 as List<Int>, value_2 as? List<Int> -> {}
        value_3 <!UNCHECKED_CAST!>as? MutableMap<Int, Int><!>, (value_2 <!UNCHECKED_CAST!>as? Map<Int, Int><!>) as MutableMap<Int, Int> -> {}
    }
}

// TESTCASE NUMBER: 11
fun case_11(value_1: Any, value_2: Int, value_3: Int, value_4: Boolean) {
    var mutableValue1 = value_2
    var mutableValue2 = value_3

    when (value_1) {
        ++mutableValue1, --mutableValue2, !value_4 -> {}
    }
}

// TESTCASE NUMBER: 12
fun case_12(value_1: Int, value_2: Int, value_3: Int, value_4: Int?) {
    var mutableValue1 = value_2
    var mutableValue2 = value_3

    when (value_1) {
        mutableValue1++, mutableValue2--, value_4!! -> {}
    }
}

// TESTCASE NUMBER: 13
fun case_13(value_1: Int, value_2: List<Int>, value_3: List<List<List<List<Int>>>>) {
    when (value_1) {
        value_2[0], value_3[0][-4][1][-1] -> {}
    }
}

// TESTCASE NUMBER: 14
fun case_14(value_1: Any, value_2: Class, value_3: Class?, value_4: Int) {
    fun __fun_1(): () -> Unit { return fun() { } }

    when (value_1) {
        funWithoutArgs(), __fun_1()(), value_2.fun_2(value_4) -> {}
        value_3?.fun_2(value_4), value_3!!.fun_2(value_4) -> {}
    }
}

// TESTCASE NUMBER: 15
fun case_15(value_1: Int, value_2: Class, value_3: Class?) {
    when (value_1) {
        value_2.prop_1, value_3?.prop_2 -> {}
        value_2::prop_1.get(), value_3!!::prop_3.get() -> {}
    }
}

// TESTCASE NUMBER: 16
fun case_16(value_1: () -> Any): Any {
    val fun_1 = fun() { return }

    return when (value_1) {
        fun() {}, fun() { return }, fun(): () -> Unit { return fun() {} }, fun_1 -> {}
        else -> {}
    }
}

// TESTCASE NUMBER: 17
fun case_17(value_1: () -> Any) {
    val lambda_1 = { 0 }

    when (value_1) {
        lambda_1, { { {} } }, { -> (Int)
            { arg: Int -> { { println(arg) } } } } -> {}
    }
}

// TESTCASE NUMBER: 18
fun case_18(value_1: Any) {
    val object_1 = object {
        val prop_1 = 1
    }

    when (value_1) {
        object {}, object {
            private fun fun_1() { }
            val prop_1 = 1
        }, object_1 -> {}
    }
}

// TESTCASE NUMBER: 19
class A {
    val prop_1 = 1
    val lambda_1 = { 1 }
    fun fun_1(): Int { return 1 }

    fun case_19(value_1: Any) {
        when (value_1) {
            this, ((this)), this::prop_1.get() -> {}
            this.prop_1, this.lambda_1() -> {}
            this::lambda_1.get()(), this.fun_1(), this::fun_1.invoke() -> {}
        }
    }
}

// TESTCASE NUMBER: 20
fun case_20(value_1: Nothing) {
    when (value_1) {
        throw Exception(), throw throw throw Exception() -> {}
    }
}

// TESTCASE NUMBER: 21
fun case_21(value_1: Nothing) {
    fun f1() {
        when (value_1) {
            return, return return return -> 2
        }
    }

    fun f2(): List<Int>? {
        when (value_1) {
            return listOf(0, 1, 2), return null -> 2
        }
    }
}

// TESTCASE NUMBER: 22
fun case_22(value_1: Nothing) {
    loop1@ while (true) {
        loop2@ while (true) {
            when (value_1) {
                continue@loop1, continue@loop2 -> 2
            }
        }
    }
}

// TESTCASE NUMBER: 23
fun case_23(value_1: Nothing) {
    loop1@ while (true) {
        loop2@ while (true) {
            when (value_1) {
                break@loop1, break@loop2 -> 2
            }
        }
    }
}

// TESTCASE NUMBER: 24
fun case_24(value_1: Nothing?) = when (value_1) {
    <!SENSELESS_COMPARISON!>throw Exception()<!>, <!SENSELESS_COMPARISON!><!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> ""<!> -> ""
    <!SENSELESS_COMPARISON!>null<!>, <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> "", throw throw throw Exception() -> ""
    else -> ""
}

/*
 * TESTCASE NUMBER: 25
 * DISCUSSION
 * ISSUES: KT-25948
 */
fun case_25(value_1: Boolean) = when (value_1) {
    true -> {}
    throw Exception(), <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> -> {}
    false, <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!>, throw throw throw Exception() -> {}
}

/*
 * TESTCASE NUMBER: 26
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-26045
 */
fun case_26(value_1: Int?, value_2: Class, value_3: Class?) {
    when (value_1) {
        value_2.prop_1, value_3?.prop_1 -> {}
        10 -> {}
    }
}

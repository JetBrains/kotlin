// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
// !DIAGNOSTICS: -UNUSED_EXPRESSION -DEBUG_INFO_SMARTCAST
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, when-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: When without bound value, various expressions in the control structure body.
 * HELPERS: typesProvider, classes, functions
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int) {
    when {
        value_1 == 1 -> true
        value_1 == 2 -> 100
        value_1 == 3 -> -.09f
        value_1 == 4 -> '.'
        value_1 == 5 -> "..."
        value_1 == 6 -> null
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int, value_2: Byte, value_3: TypesProvider) {
    when {
        value_1 == 1 -> -.09 % 10L
        value_1 == 3 -> value_2 / -5
        value_1 == 2 -> value_3.getChar() - 11 + 90
    }
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Int, value_2: Boolean, value_3: Long) {
    when {
        value_1 == 1 -> value_2
        value_1 == 2 -> !value_2
        value_1 == 3 -> getBoolean() && value_2
        value_1 == 5 -> getChar() != 'a'
        value_1 == 6 -> Out<Int>() === getAny()
        value_1 == 7 -> value_3 <= 11
    }
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Int, value_2: String, value_3: String) {
    when {
        value_1 == 1 -> "..." + value_2 + "" + "$value_3" + "..."
        value_1 == 2 -> value_2 + getString()
    }
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Int, value_2: Int, value_3: Boolean?) {
    when {
        value_1 == 1 -> when {
            value_2 > 1000 -> "1"
            value_2 > 100 -> "2"
            else -> "3"
        }
        value_1 == 2 -> when {
            value_2 > 1000 -> "1"
            value_2 > 100 -> "2"
        }
        value_1 == 3 -> when {}
        value_1 == 4 -> when (value_3) {
            true -> "1"
            false -> "2"
            null -> "3"
        }
        value_1 == 5 -> when (value_3) {
            true -> "1"
            false -> "2"
            else -> ""
        }
        value_1 == 6 -> when (value_3) {
            else -> ""
        }
    }
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Int, value_2: Int, value_3: Boolean?) = when {
    value_1 == 1 -> when {
        value_2 > 1000 -> 1
        value_2 > 100 -> 2
        else -> 3
    }
    else -> when (value_3) {
        true -> 1
        false -> 2
        null -> 3
    }
}

// TESTCASE NUMBER: 7
fun case_7(value_1: Int, value_2: Int, value_3: Boolean?) {
    when {
        value_1 == 1 -> if (value_2 > 1000) "1"
        value_1 == 2 -> if (value_2 > 1000) "1"
            else "2"
        value_1 == 3 -> if (value_2 < 100) "1"
            else if (value_2 < 10) "2"
            else "4"
        value_1 == 4 -> if (value_3 == null) "1"
            else if (value_3) "2"
            else if (!value_3) "3"
    }
}

// TESTCASE NUMBER: 8
fun case_8(value_1: Int, value_2: Int) = when {
    value_1 == 1 -> if (value_2 > 1000) "1"
        else "2"
    else -> if (value_2 < 100) "1"
        else if (value_2 < 10) "2"
        else "4"
}

/*
 * TESTCASE NUMBER: 9
 * ISSUES: KT-37249
 */
fun case_9(value_1: Int, value_2: String, value_3: String) = when {
    value_1 == 1 -> try { 4 } catch (e: Exception) { 5 }
    value_1 == 2 -> try { throw Exception() } catch (e: Exception) { value_2 }
    else -> try { throw Exception() } catch (e: Exception) { {value_3} } finally { }
}

// TESTCASE NUMBER: 10
fun case_10(value_1: Int, value_2: String?, value_3: String?) {
    when {
        value_1 == 1 -> value_2 ?: true
        value_1 == 2 -> value_2 ?: value_3 ?: true
        value_1 == 3 -> value_2!! <!USELESS_ELVIS!>?: true<!>
    }
}

// TESTCASE NUMBER: 11
fun case_11(value_1: Int) {
    when {
        value_1 == 1 -> 1..10
        value_1 == 2 -> -100L..100L
        value_1 == 3 -> -getInt()..getLong()
    }
}

// TESTCASE NUMBER: 12
fun case_12(value_1: Int, value_2: Collection<Int>, value_3: Collection<Int>?) {
    when {
        value_1 == 1 -> value_2 as List<Int>
        value_1 == 2 -> value_2 as? List<Int>
        value_1 == 3 -> value_3 <!UNCHECKED_CAST!>as? MutableMap<Int, Int><!>
        value_1 == 4 -> (value_2 <!UNCHECKED_CAST!>as? Map<Int, Int><!>) as MutableMap<Int, Int>
    }
}

// TESTCASE NUMBER: 13
fun case_13(value_1: Int, value_2: Int, value_3: Int, value_4: Boolean) {
    var mutablevalue_2 = value_2
    var mutablevalue_3 = value_3

    when {
        value_1 == 1 -> ++mutablevalue_2
        value_1 == 2 -> --mutablevalue_3
        value_1 == 3 -> !value_4
    }
}

// TESTCASE NUMBER: 14
fun case_14(value_1: Int, value_2: Int, value_3: Int, value_4: Boolean?) {
    var mutablevalue_2 = value_2
    var mutablevalue_3 = value_3

    when {
        value_1 == 1 -> mutablevalue_2++
        value_1 == 2 -> mutablevalue_3--
        value_1 == 3 -> value_4!!
    }
}

// TESTCASE NUMBER: 15
fun case_15(value_1: Int, value_2: List<Int>, value_3: List<List<List<List<Int>>>>) {
    when {
        value_1 == 1 -> value_2[0]
        value_1 == 2 -> value_3[0][-4][1][-1]
    }
}

// TESTCASE NUMBER: 16
fun case_16(value_1: Int, value_2: Class, value_3: Class?, value_4: Int) {
    fun __fun_1(): () -> Unit { return fun() { } }

    when {
        value_1 == 1 -> funWithoutArgs()
        value_1 == 2 -> __fun_1()()
        value_1 == 3 -> value_2.fun_2(value_4)
        value_1 == 4 -> value_3?.fun_2(value_4)
        value_1 == 5 -> value_3!!.fun_2(value_4)
    }
}

// TESTCASE NUMBER: 17
fun case_17(value_1: Int, value_2: Class, value_3: Class?) {
    when {
        value_1 == 1 -> value_2.prop_1
        value_1 == 2 -> value_3?.prop_1
        value_1 == 3 -> value_2::prop_1.get()
        value_1 == 4 -> value_3!!::prop_3.get()
    }
}

// TESTCASE NUMBER: 18
fun case_18(value_1: Int) {
    val fun_1 = fun(): Int { return 0 }

    when {
        value_1 == 1 -> fun() {}
        value_1 == 2 -> fun(): Int { return 0 }
        value_1 == 3 -> fun(): () -> Unit { return fun() {} }
        value_1 == 4 -> fun_1
    }
}

// TESTCASE NUMBER: 19
fun case_19(value_1: Int): () -> Any {
    val lambda_1 = { 0 }

    return when {
        value_1 == 1 -> lambda_1
        value_1 == 2 -> { { {} } }
        else -> { -> (Int)
            { arg: Int -> { { println(arg) } } }
        }
    }
}

// TESTCASE NUMBER: 20
fun case_20(value_1: Int) {
    val object_1 = object {
        val prop_1 = 1
    }

    when {
        value_1 == 1 -> object {}
        value_1 == 2 -> object {
            private fun fun_1() { }
            val prop_1 = 1
        }
        value_1 == 3 -> object_1
    }
}

// TESTCASE NUMBER: 21
class A {
    val prop_1 = 1
    val lambda_1 = { 1 }
    fun fun_1(): Int { return 1 }

    fun case_21(value_1: Int) {
        when {
            value_1 == 1 -> this
            value_1 == 2 -> ((this))
            value_1 == 3 -> this::prop_1.get()
            value_1 == 4 -> this.prop_1
            value_1 == 5 -> this.lambda_1()
            value_1 == 6 -> this::lambda_1.get()()
            value_1 == 7 -> this.fun_1()
            value_1 == 8 -> this::fun_1.invoke()
        }
    }
}

// TESTCASE NUMBER: 22
fun case_22(value_1: Int) {
    when {
        value_1 == 1 -> throw Exception()
        value_1 == 2 -> throw throw throw Exception()
    }
}

// TESTCASE NUMBER: 23
fun case_23(value_1: Int) {
    fun r_1() {
        when {
            value_1 == 1 -> return
            value_1 == 2 -> return return return
        }
    }

    fun r_2(): List<Int>? {
        when {
            value_1 == 1 -> return listOf(0, 1, 2)
            value_1 == 2 -> return null
        }

        return null
    }
}

// TESTCASE NUMBER: 24
fun case_24(value_1: Int) {
    loop1@ while (true) {
        loop2@ while (true) {
            when {
                value_1 == 1 -> continue@loop1
                value_1 == 2 -> continue@loop2
            }
        }
    }
}

// TESTCASE NUMBER: 25
fun case_25(value_1: Int) {
    loop1@ while (true) {
        loop2@ while (true) {
            when {
                value_1 == 1 -> break@loop1
                value_1 == 2 -> break@loop2
            }
        }
    }
}

// !WITH_BASIC_TYPES
// !WITH_FUNCTIONS
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 7
 SENTENCE: [5] Any other expression.
 NUMBER: 1
 DESCRIPTION: 'When' with enumeration of the different variants of expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with condition as literals.
fun case_1(value: Any?) {
    when (value) {
        true -> {}
        100 -> {}
        -.09f -> {}
        '.' -> {}
        "..." -> {}
        null -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as arithmetic expressions.
fun case_2(value: Number, value1: Int) {
    when (value) {
        -.09 % 10L -> {}
        value1 / -5 -> {}
        getByte(99) - 11 + 90 -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as boolean expressions (logical, equality and comparison).
fun case_3(value: Boolean, value1: Boolean, value2: Long) {
    when (value) {
        value1 -> {}
        !value1 -> {}
        getBoolean() && value1 -> {}
        getChar(10) != 'a' -> {}
        getList() === getAny() -> {}
        value2 <= 11 -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as concatenations.
fun case_4(value: String, value1: String, value2: String) {
    when (value) {
        "..." + value1 + "" + "$value2" + "..." -> {}
        value1 + getString() -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as when expression.
fun case_5(value: Int, value1: Int, value2: Boolean?) {
    when (value) {
        when {
            value1 > 1000 -> 1
            value1 > 100 -> 2
            else -> 3
        } -> {}
        when (value2) {
            true -> 1
            false -> 2
            null -> 3
        } -> {}
        when (value2!!) {
            true -> 1
            false -> 2
        } -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as if expression.
fun case_6(value: Int, value1: Int) {
    when (value) {
        if (value1 > 1000) 1
        else 2 -> {}
        if (value1 < 100) 1
        else if (value1 < 10) 2
        else 3 -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as try expression.
fun case_7(value: Any, value1: String, value2: String) {
    when (value) {
        try { 4 } catch (e: Exception) { 5 } -> {}
        try { throw Exception() } catch (e: Exception) { value1 } -> {}
        try { throw Exception() } catch (e: Exception) { {value2} } finally { } -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as elvis operator expression.
fun case_8(value: Int, value1: Int?, value2: Int?) {
    when (value) {
        value1 ?: 0 -> {}
        value1 ?: value2 ?: 0 -> {}
        value1!! <!USELESS_ELVIS!>?: 0<!> -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as range expression.
fun case_9(value: Any) {
    when (value) {
        1..10 -> {}
        -100L..100L -> {}
        -getInt()..getLong() -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as cast expression.
fun case_10(value: Collection<Int>, value1: Collection<Int>, value2: Collection<Int>?) {
    when (value) {
        value1 as MutableList<Int> -> {}
        value1 <!USELESS_CAST!>as? MutableList<Int><!> -> {}
        value2 <!UNCHECKED_CAST!>as? MutableMap<Int, Int><!> -> {}
        (value1 <!UNCHECKED_CAST!>as? Map<Int, Int><!>) as MutableMap<Int, Int> -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as prefix operator expression.
fun case_11(value: Any, value1: Int, value2: Int, value3: Boolean) {
    var mutableValue1 = value1
    var mutableValue2 = value2

    when (value) {
        ++mutableValue1 -> {}
        --mutableValue2 -> {}
        !value3 -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as postfix operator expression.
fun case_12(value: Int, value1: Int, value2: Int, value3: Int?) {
    var mutableValue1 = value1
    var mutableValue2 = value2

    when (value) {
        <!UNUSED_CHANGED_VALUE!>mutableValue1++<!> -> {}
        <!UNUSED_CHANGED_VALUE!>mutableValue2--<!> -> {}
        value3!! -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as indexing expression.
fun case_13(value: Int, value1: List<Int>, value2: List<List<List<List<Int>>>>) {
    when (value) {
        value1[0] -> {}
        value2[0][-4][1][-1] -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as call expression.
fun case_14(value: Any, value1: _Class, value2: _Class?, value3: Int) {
    fun __fun_1(): () -> Any { return fun() { } }

    when (value) {
        _funWithoutArgs() -> {}
        __fun_1()() -> {}
        value1.fun_2(value3) -> {}
        value2?.fun_2(value3) -> {}
        value2!!.fun_2(value3) -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as property access expression.
fun case_15(value: Int, value1: _Class, value2: _Class?) {
    when (value) {
        value1.prop_1 -> {}
        value2?.prop_2 -> {}
        value1::prop_1.get() -> {}
        value2!!::prop_3.get() -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as fun literal.
fun case_16(value: () -> Any): Any {
    val fun_1 = fun() { return }

    return when (value) {
        fun() {} -> {}
        fun() { return } -> {}
        fun(): () -> Unit { return fun() {} } -> {}
        fun_1 -> {}
        else -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as lambda literal.
fun case_17(value: () -> Any) {
    val lambda_1 = { 0 }

    when (value) {
        lambda_1 -> {}
        { { {} } } -> {}
        { -> (Int)
            { arg: Int -> { { println(arg) } } }
        } -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as object literal.
fun case_18(value: Any) {
    val object_1 = object {
        val prop_1 = 1
    }

    when (value) {
        object {} -> {}
        object {
            private fun fun_1() { }
            val prop_1 = 1
        } -> {}
        object_1 -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as this expression.
class A {
    val prop_1 = 1
    val lambda_1 = { 1 }
    fun fun_1(): Int { return 1 }

    fun case_19(value: Any) {
        when (value) {
            this -> {}
            ((this)) -> {}
            this::prop_1.get() -> {}
            this.prop_1 -> {}
            this.lambda_1() -> {}
            this::lambda_1.get()() -> {}
            this.fun_1() -> {}
            this::fun_1.invoke() -> {}
        }
    }
}

// CASE DESCRIPTION: 'When' with condition as throw expression.
fun case_20(value: Nothing) {
    when (value) {
        <!UNREACHABLE_CODE!>throw Exception() -> {}<!>
        <!UNREACHABLE_CODE!>throw throw throw Exception() -> {}<!>
    }
}

// CASE DESCRIPTION: 'When' with condition as return expression.
fun case_21(value: Nothing) {
    fun r_1() {
        when (value) {
            <!UNREACHABLE_CODE!>return -> 1<!>
            <!UNREACHABLE_CODE!>return return return -> 2<!>
        }
    }

    fun r_2(): List<Int>? {
        when (value) {
            <!UNREACHABLE_CODE!>return listOf(0, 1, 2) -> 1<!>
            <!UNREACHABLE_CODE!>return null -> 2<!>
        }
    }
}

// CASE DESCRIPTION: 'When' with condition as continue expression.
fun case_22(value: Nothing) {
    loop1@ while (true) {
        loop2@ while (true) {
            when (value) {
                <!UNREACHABLE_CODE!>continue@loop1 -> 1<!>
                <!UNREACHABLE_CODE!>continue@loop2 -> 2<!>
            }
        }
    }
}

// CASE DESCRIPTION: 'When' with condition as break expression.
fun case_23(value: Nothing) {
    loop1@ while (true) {
        loop2@ while (true) {
            when (value) {
                <!UNREACHABLE_CODE!>break@loop1 -> 1<!>
                <!UNREACHABLE_CODE!>break@loop2 -> 2<!>
            }
        }
    }
}

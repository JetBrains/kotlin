// !WITH_BASIC_TYPES
// !WITH_FUNCTIONS
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 7
 SENTENCE: [5] Any other expression.
 NUMBER: 1
 DESCRIPTION: 'When' with enumeration of the different variants of expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with condition as literals.
fun case_1(value_1: Any?) {
    when (value_1) {
        true -> {}
        100 -> {}
        -.09f -> {}
        '.' -> {}
        "..." -> {}
        null -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as arithmetic expressions.
fun case_2(value_1: Number, value_2: Int) {
    when (value_1) {
        -.09 % 10L -> {}
        value_2 / -5 -> {}
        getByte(99) - 11 + 90 -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as boolean expressions (logical, equality and comparison).
fun case_3(value_1: Boolean, value_2: Boolean, value_3: Long) {
    when (value_1) {
        value_2 -> {}
        !value_2 -> {}
        getBoolean() && value_2 -> {}
        getChar(10) != 'a' -> {}
        getList() === getAny() -> {}
        value_3 <= 11 -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as concatenations.
fun case_4(value_1: String, value_2: String, value_3: String) {
    when (value_1) {
        "..." + value_2 + "" + "$value_3" + "..." -> {}
        value_2 + getString() -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as when expression.
fun case_5(value_1: Int, value_2: Int, value_3: Boolean?) {
    when (value_1) {
        when {
            value_2 > 1000 -> 1
            value_2 > 100 -> 2
            else -> 3
        } -> {}
        when (value_3) {
            true -> 1
            false -> 2
            null -> 3
        } -> {}
        when (value_3!!) {
            true -> 1
            false -> 2
        } -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as if expression.
fun case_6(value_1: Int, value_2: Int) {
    when (value_1) {
        if (value_2 > 1000) 1
        else 2 -> {}
        if (value_2 < 100) 1
        else if (value_2 < 10) 2
        else 3 -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as try expression.
fun case_7(value_1: Any, value_2: String, value_3: String) {
    when (value_1) {
        try { 4 } catch (e: Exception) { 5 } -> {}
        try { throw Exception() } catch (e: Exception) { value_2 } -> {}
        try { throw Exception() } catch (e: Exception) { {value_3} } finally { } -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as elvis operator expression.
fun case_8(value_1: Int, value_2: Int?, value_3: Int?) {
    when (value_1) {
        value_2 ?: 0 -> {}
        value_2 ?: value_3 ?: 0 -> {}
        value_2!! <!USELESS_ELVIS!>?: 0<!> -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as range expression.
fun case_9(value_1: Any) {
    when (value_1) {
        1..10 -> {}
        -100L..100L -> {}
        -getInt()..getLong() -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as cast expression.
fun case_10(value_1: Collection<Int>, value_2: Collection<Int>, value_3: Collection<Int>?) {
    when (value_1) {
        value_2 as MutableList<Int> -> {}
        value_2 <!USELESS_CAST!>as? MutableList<Int><!> -> {}
        value_3 <!UNCHECKED_CAST!>as? MutableMap<Int, Int><!> -> {}
        (value_2 <!UNCHECKED_CAST!>as? Map<Int, Int><!>) as MutableMap<Int, Int> -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as prefix operator expression.
fun case_11(value_1: Any, value_2: Int, value_3: Int, value_4: Boolean) {
    var mutableValue1 = value_2
    var mutableValue2 = value_3

    when (value_1) {
        ++mutableValue1 -> {}
        --mutableValue2 -> {}
        !value_4 -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as postfix operator expression.
fun case_12(value_1: Int, value_2: Int, value_3: Int, value_4: Int?) {
    var mutableValue1 = value_2
    var mutableValue2 = value_3

    when (value_1) {
        <!UNUSED_CHANGED_VALUE!>mutableValue1++<!> -> {}
        <!UNUSED_CHANGED_VALUE!>mutableValue2--<!> -> {}
        value_4!! -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as indexing expression.
fun case_13(value_1: Int, value_2: List<Int>, value_3: List<List<List<List<Int>>>>) {
    when (value_1) {
        value_2[0] -> {}
        value_3[0][-4][1][-1] -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as call expression.
fun case_14(value_1: Any, value_2: _Class, value_3: _Class?, value_4: Int) {
    fun __fun_1(): () -> Any { return fun() { } }

    when (value_1) {
        _funWithoutArgs() -> {}
        __fun_1()() -> {}
        value_2.fun_2(value_4) -> {}
        value_3?.fun_2(value_4) -> {}
        value_3!!.fun_2(value_4) -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as property access expression.
fun case_15(value_1: Int, value_2: _Class, value_3: _Class?) {
    when (value_1) {
        value_2.prop_1 -> {}
        value_3?.prop_2 -> {}
        value_2::prop_1.get() -> {}
        value_3!!::prop_3.get() -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as fun literal.
fun case_16(value_1: () -> Any): Any {
    val fun_1 = fun() { return }

    return when (value_1) {
        fun() {} -> {}
        fun() { return } -> {}
        fun(): () -> Unit { return fun() {} } -> {}
        fun_1 -> {}
        else -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as lambda literal.
fun case_17(value_1: () -> Any) {
    val lambda_1 = { 0 }

    when (value_1) {
        lambda_1 -> {}
        { { {} } } -> {}
        { -> (Int)
            { arg: Int -> { { println(arg) } } }
        } -> {}
    }
}

// CASE DESCRIPTION: 'When' with condition as object literal.
fun case_18(value_1: Any) {
    val object_1 = object {
        val prop_1 = 1
    }

    when (value_1) {
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

    fun case_19(value_1: Any) {
        when (value_1) {
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
fun case_20(value_1: Nothing) {
    when (value_1) {
        <!UNREACHABLE_CODE!>throw Exception() -> {}<!>
        <!UNREACHABLE_CODE!>throw throw throw Exception() -> {}<!>
    }
}

// CASE DESCRIPTION: 'When' with condition as return expression.
fun case_21(value_1: Nothing) {
    fun r_1() {
        when (value_1) {
            <!UNREACHABLE_CODE!>return -> 1<!>
            <!UNREACHABLE_CODE!>return return return -> 2<!>
        }
    }

    fun r_2(): List<Int>? {
        when (value_1) {
            <!UNREACHABLE_CODE!>return listOf(0, 1, 2) -> 1<!>
            <!UNREACHABLE_CODE!>return null -> 2<!>
        }
    }
}

// CASE DESCRIPTION: 'When' with condition as continue expression.
fun case_22(value_1: Nothing) {
    loop1@ while (true) {
        loop2@ while (true) {
            when (value_1) {
                <!UNREACHABLE_CODE!>continue@loop1 -> 1<!>
                <!UNREACHABLE_CODE!>continue@loop2 -> 2<!>
            }
        }
    }
}

// CASE DESCRIPTION: 'When' with condition as break expression.
fun case_23(value_1: Nothing) {
    loop1@ while (true) {
        loop2@ while (true) {
            when (value_1) {
                <!UNREACHABLE_CODE!>break@loop1 -> 1<!>
                <!UNREACHABLE_CODE!>break@loop2 -> 2<!>
            }
        }
    }
}

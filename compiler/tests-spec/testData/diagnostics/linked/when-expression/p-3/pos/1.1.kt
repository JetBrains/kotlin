// !DIAGNOSTICS: -UNUSED_EXPRESSION -DEBUG_INFO_SMARTCAST
// !WITH_BASIC_TYPES
// !WITH_CLASSES
// !WITH_FUNCTIONS

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 3
 SENTENCE: [1] When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 1
 DESCRIPTION: 'When' without bound value and with different variants of expressions in the control structure body.
 */

// CASE DESCRIPTION: 'When' with control structure body as literals.
fun case_1(value: Int) {
    when {
        value == 1 -> true
        value == 2 -> 100
        value == 3 -> -.09f
        value == 4 -> '.'
        value == 5 -> "..."
        value == 6 -> null
    }
}

// CASE DESCRIPTION: 'When' with control structure body as arithmetic expressions.
fun case_2(value: Int, value1: Byte, value2: _BasicTypesProvider) {
    when {
        value == 1 -> -.09 % 10L
        value == 3 -> value1 / -5
        value == 2 -> value2.getChar(99) - 11 + 90
    }
}

// CASE DESCRIPTION: 'When' with control structure body as boolean expressions (logical, equality and comparison).
fun case_3(value: Int, value1: Boolean, value2: Long) {
    when {
        value == 1 -> value1
        value == 2 -> !value1
        value == 3 -> getBoolean() && value1
        value == 5 -> getChar(10) != 'a'
        value == 6 -> getList() === getAny()
        value == 7 -> value2 <= 11
    }
}

// CASE DESCRIPTION: 'When' with control structure body as concatenations.
fun case_4(value: Int, value1: String, value2: String) {
    when {
        value == 1 -> "..." + value1 + "" + "$value2" + "..."
        value == 2 -> value1 + getString()
    }
}

// CASE DESCRIPTION: 'When' with control structure body as when expression.
fun case_5(value: Int, value1: Int, value2: Boolean?) {
    when {
        value == 1 -> when {
            value1 > 1000 -> "1"
            value1 > 100 -> "2"
            else -> "3"
        }
        value == 2 -> when {
            value1 > 1000 -> "1"
            value1 > 100 -> "2"
        }
        value == 3 -> when {}
        value == 4 -> when (value2) {
            true -> "1"
            false -> "2"
            null -> "3"
        }
        value == 5 -> when (value2) {
            true -> "1"
            false -> "2"
        }
        value == 6 -> when (value2) {}
    }
}

// CASE DESCRIPTION: 'When' as expression with control structure body as when expression (must be exhaustive).
fun case_6(value: Int, value1: Int, value2: Boolean?) = when {
    value == 1 -> when {
        value1 > 1000 -> 1
        value1 > 100 -> 2
        else -> 3
    }
    else -> when (value2) {
        true -> 1
        false -> 2
        null -> 3
    }
}

// CASE DESCRIPTION: 'When' with control structure body as if expression.
fun case_7(value: Int, value1: Int, value2: Boolean?) {
    when {
        value == 1 -> if (value1 > 1000) "1"
        value == 2 -> if (value1 > 1000) "1"
            else "2"
        value == 3 -> if (value1 < 100) "1"
            else if (value1 < 10) "2"
            else "4"
        value == 4 -> if (value2 == null) "1"
            else if (value2) "2"
            else if (!value2) "3"
    }
}

// CASE DESCRIPTION: 'When' as expression with control structure body as if expression (must be exhaustive).
fun case_8(value: Int, value1: Int) = when {
    value == 1 -> if (value1 > 1000) "1"
        else "2"
    else -> if (value1 < 100) "1"
        else if (value1 < 10) "2"
        else "4"
}

// CASE DESCRIPTION: 'When' with control structure body as try expression.
fun case_9(value: Int, value1: String, value2: String) = when {
    value == 1 -> <!IMPLICIT_CAST_TO_ANY!>try { 4 } catch (e: Exception) { 5 }<!>
    value == 2 -> <!IMPLICIT_CAST_TO_ANY!>try { throw Exception() } catch (e: Exception) { value1 }<!>
    else -> <!IMPLICIT_CAST_TO_ANY!>try { throw Exception() } catch (e: Exception) { {value2} } finally { }<!>
}

// CASE DESCRIPTION: 'When' with control structure body as elvis operator expression.
fun case_10(value: Int, value1: String?, value2: String?) {
    when {
        value == 1 -> value1 ?: true
        value == 2 -> value1 ?: value2 ?: true
        value == 3 -> value1!! <!USELESS_ELVIS!>?: true<!>
    }
}

// CASE DESCRIPTION: 'When' with control structure body as range expression.
fun case_11(value: Int) {
    when {
        value == 1 -> 1..10
        value == 2 -> -100L..100L
        value == 3 -> -getInt()..getLong()
    }
}

// CASE DESCRIPTION: 'When' with control structure body as cast expression.
fun case_12(value: Int, value1: Collection<Int>, value2: Collection<Int>?) {
    when {
        value == 1 -> value1 as MutableList<Int>
        value == 2 -> value1 as? MutableList<Int>
        value == 3 -> value2 <!UNCHECKED_CAST!>as? MutableMap<Int, Int><!>
        value == 4 -> (value1 <!UNCHECKED_CAST!>as? Map<Int, Int><!>) as MutableMap<Int, Int>
    }
}

// CASE DESCRIPTION: 'When' with control structure body as prefix operator expression.
fun case_13(value: Int, value1: Int, value2: Int, value3: Boolean) {
    var mutableValue1 = value1
    var mutableValue2 = value2

    when {
        value == 1 -> ++mutableValue1
        value == 2 -> --mutableValue2
        value == 3 -> !value3
    }
}

// CASE DESCRIPTION: 'When' with control structure body as postfix operator expression.
fun case_14(value: Int, value1: Int, value2: Int, value3: Boolean?) {
    var mutableValue1 = value1
    var mutableValue2 = value2

    when {
        value == 1 -> <!UNUSED_CHANGED_VALUE!>mutableValue1++<!>
        value == 2 -> <!UNUSED_CHANGED_VALUE!>mutableValue2--<!>
        value == 3 -> value3!!
    }
}

// CASE DESCRIPTION: 'When' with control structure body as indexing expression.
fun case_15(value: Int, value1: List<Int>, value2: List<List<List<List<Int>>>>) {
    when {
        value == 1 -> value1[0]
        value == 2 -> value2[0][-4][1][-1]
    }
}

// CASE DESCRIPTION: 'When' with control structure body as call expression.
fun case_16(value: Int, value1: _Class, value2: _Class?, value3: Int) {
    fun __fun_1(): () -> Unit { return fun() { } }

    when {
        value == 1 -> _funWithoutArgs()
        value == 2 -> __fun_1()()
        value == 3 -> value1.fun_2(value3)
        value == 4 -> value2?.fun_2(value3)
        value == 5 -> value2!!.fun_2(value3)
    }
}

// CASE DESCRIPTION: 'When' with control structure body as property access expression.
fun case_17(value: Int, value1: _Class, value2: _Class?) {
    when {
        value == 1 -> value1.prop_1
        value == 2 -> value2?.prop_1
        value == 3 -> value1::prop_1.get()
        value == 4 -> value2!!::prop_3.get()
    }
}

// CASE DESCRIPTION: 'When' with control structure body as fun literal.
fun case_18(value: Int) {
    val fun_1 = fun(): Int { return 0 }

    when {
        value == 1 -> fun() {}
        value == 2 -> fun(): Int { return 0 }
        value == 3 -> fun(): () -> Unit { return fun() {} }
        value == 4 -> fun_1
    }
}

// CASE DESCRIPTION: 'When' with control structure body as lambda literal.
fun case_19(value: Int): () -> Any {
    val lambda_1 = { 0 }

    return when {
        value == 1 -> lambda_1
        value == 2 -> { { {} } }
        else -> { -> (Int)
            { arg: Int -> { { println(arg) } } }
        }
    }
}

// CASE DESCRIPTION: 'When' with control structure body as object literal.
fun case_20(value: Int) {
    val object_1 = object {
        val prop_1 = 1
    }

    when {
        value == 1 -> object {}
        value == 2 -> object {
            private fun fun_1() { }
            val prop_1 = 1
        }
        value == 3 -> object_1
    }
}

// CASE DESCRIPTION: 'When' with control structure body as this expression.
class A {
    val prop_1 = 1
    val lambda_1 = { 1 }
    fun fun_1(): Int { return 1 }

    fun case_21(value: Int) {
        when {
            value == 1 -> this
            value == 2 -> ((this))
            value == 3 -> this::prop_1.get()
            value == 4 -> this.prop_1
            value == 5 -> this.lambda_1()
            value == 6 -> this::lambda_1.get()()
            value == 7 -> this.fun_1()
            value == 8 -> this::fun_1.invoke()
        }
    }
}

// CASE DESCRIPTION: 'When' with control structure body as throw expression.
fun case_22(value: Int) {
    when {
        value == 1 -> throw Exception()
        value == 2 -> throw throw throw Exception()
    }
}

// CASE DESCRIPTION: 'When' with control structure body as return expression.
fun case_23(value: Int) {
    fun r_1() {
        when {
            value == 1 -> return
            value == 2 -> <!UNREACHABLE_CODE!>return return<!> return
        }
    }

    fun r_2(): List<Int>? {
        when {
            value == 1 -> return listOf(0, 1, 2)
            value == 2 -> return null
        }

        return null
    }
}

// CASE DESCRIPTION: 'When' with control structure body as continue expression.
fun case_24(value: Int) {
    loop1@ while (true) {
        loop2@ while (true) {
            when {
                value == 1 -> continue@loop1
                value == 2 -> continue@loop2
            }
        }
    }
}

// CASE DESCRIPTION: 'When' with control structure body as break expression.
fun case_25(value: Int) {
    loop1@ while (true) {
        loop2@ while (true) {
            when {
                value == 1 -> break@loop1
                value == 2 -> break@loop2
            }
        }
    }
}

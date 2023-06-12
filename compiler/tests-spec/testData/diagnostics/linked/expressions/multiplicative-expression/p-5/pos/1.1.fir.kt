// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, multiplicative-expression -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: expressions, multiplicative-expression -> paragraph 5 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: The return type of these functions is not restricted.
 * HELPERS: checkType
 */
// TESTCASE NUMBER: 1
class Case1(var a: Int) {
    operator fun times(o: Int): Any? { TODO() }
    operator fun times(o: Case1):Any? { TODO() }
    operator fun div(o: Int): Any? { TODO() }
    operator fun div(o: Case1): Any? { TODO() }
    operator fun rem(o: Int): Any? { TODO() }
    operator fun rem(o: Case1): Any? { TODO() }
}

fun case1() {
    val a = Case1(1) * 1
    val b = Case1(1) * Case1( 1)
    val c = Case1(1) / 1
    val d = Case1(1) / Case1( 1)
    val e = Case1(1) % 1
    val f = Case1(1) % Case1( 1)

    a checkType { check<Any?>() }
    b checkType { check<Any?>() }
    c checkType { check<Any?>() }
    d checkType { check<Any?>() }
    e checkType { check<Any?>() }
    f checkType { check<Any?>() }
}

// TESTCASE NUMBER: 2
class Case2(var a: Int) {
    operator fun times(o: Int): Nothing? { TODO() }
    operator fun times(o: Case2):Nothing? { TODO() }
    operator fun div(o: Int): Nothing? { TODO() }
    operator fun div(o: Case2): Nothing? { TODO() }
    operator fun rem(o: Int): Nothing? { TODO() }
    operator fun rem(o: Case2): Nothing? { TODO() }
}

fun case2() {
    val a = Case2(1) * 1
    val b = Case2(1) * Case2( 1)
    val c = Case2(1) / 1
    val d = Case2(1) / Case2( 1)
    val e = Case2(1) % 1
    val f = Case2(1) % Case2( 1)

    a checkType { check<Nothing?>() }
    b checkType { check<Nothing?>() }
    c checkType { check<Nothing?>() }
    d checkType { check<Nothing?>() }
    e checkType { check<Nothing?>() }
    f checkType { check<Nothing?>() }
}

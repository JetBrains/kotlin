// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 10
 * PRIMARY LINKS: expressions, try-expressions -> paragraph 2 -> sentence 1
 * expressions, try-expressions -> paragraph 2 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: smartcast absence coz of operator assignment
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case1() {
    var c: SomeEnum1? = null
    c checkType { check<SomeEnum1?>() }
    c -= try { SomeEnum1.FOO } catch (e: Exception) { throw Exception() }
    c checkType { check<SomeEnum1?>() }
}

enum class SomeEnum1(val flag: Boolean)  {
    FOO(false), BAR(true)
}

operator fun <T> SomeEnum1?.minus(x: T): T = TODO()


// TESTCASE NUMBER: 2
fun case2() {
    var c: Intf2? = null
    c checkType { check<Intf2?>() }
    c *= try { SomeEnum2.FOO } catch (e: Exception) { return }
    c checkType { check<Intf2?>() }
}

enum class SomeEnum2(val flag: Boolean) : Intf2 {
    FOO(false), BAR(true)
}
interface Intf2

operator fun <T> Intf2?.times(x: T): T = TODO()


// TESTCASE NUMBER: 3
fun case3() {
    var c: SomeEnum3? = null
    c checkType { check<SomeEnum3?>() }
    c /= try { SomeEnum3.FOO } catch (e: Exception) { SomeEnum3.BAR }
    c checkType { check<SomeEnum3?>() }
}

enum class SomeEnum3(val flag: Boolean)  {
    FOO(false), BAR(true)
}

operator fun <T> SomeEnum3?.div(x: T): T = TODO()



// TESTCASE NUMBER: 14
fun case14() {
    var c: SomeEnum4? = null
    c checkType { check<SomeEnum4?>() }
    c %= if(SomeEnum4.BAR.flag) SomeEnum4.BAR else SomeEnum4.FOO
    c checkType { check<SomeEnum4?>() }
}

enum class SomeEnum4(val flag: Boolean)  {
    FOO(false), BAR(true)
}

operator fun <T> SomeEnum4?.rem(x: T): T = TODO()

// TESTCASE NUMBER: 5
fun case5() {
    var c: SomeEnum5 = SomeEnum5.FOO
    c checkType { check<SomeEnum5>() }
    c += SomeEnum5.FOO
    c checkType { check<SomeEnum5>() }
}

enum class SomeEnum5(val flag: Boolean)  {
    FOO(false), BAR(true)
}

operator fun <T> SomeEnum5.plus(x: T): T = TODO()


// TESTCASE NUMBER: 6
fun case6() {
    var c: Any = SomeEnum6.FOO
    c checkType { check<Any>() }
    c += try { SomeEnum6.FOO } catch (e: Exception) { throw Exception() }
    c checkType { check<Any>() }
}

enum class SomeEnum6(val flag: Boolean)  {
    FOO(false), BAR(true)
}

operator fun <T> Any.plus(x: T): T = TODO()

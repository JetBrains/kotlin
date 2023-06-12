// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-464
 * MAIN LINK: type-system, subtyping, subtyping-for-intersection-types -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: type-system, subtyping, subtyping-for-intersection-types -> paragraph 2 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: intersection type inferred for enum classes
 * HELPERS: checkType, functions
 */

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-39405
 */
interface I{

}
enum class EA : I {
    A
}
enum class EB : I {
    B
}
fun case1(a: Any) {
    val x1 = if (true) EA.A else EB.B
    checkSubtype<Enum<*>>(x1)
    checkSubtype<I>(x1)
    <!DEBUG_INFO_EXPRESSION_TYPE("I & kotlin.Enum<*>")!>x1<!>

    val x2 = if (true) EB.B else EA.A
    checkSubtype<Enum<*>>(x2)
    checkSubtype<I>(x2)
    <!DEBUG_INFO_EXPRESSION_TYPE("I & kotlin.Enum<*>")!>x2<!>

    val x3 = when(a){
        is Int -> EA.A
        else -> EB.B
    }
    checkSubtype<Enum<*>>(x3)
    checkSubtype<I>(x3)
    <!DEBUG_INFO_EXPRESSION_TYPE("I & kotlin.Enum<*>")!>x3<!>

}
/*
 * TESTCASE NUMBER: 2
 */
enum class A  {
    A
}
interface IB
enum class B : IB {
    B
}
fun case2(a: Any) {
    val x1 = if (true) A.A else B.B
    checkSubtype<Enum<*>>(x1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Enum<*>")!>x1<!>

    val x2 = if (true) B.B else A.A
    checkSubtype<Enum<*>>(x2)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Enum<*>")!>x2<!>

    val x3 = when(a){
        is Int -> A.A
        else -> B.B
    }
    checkSubtype<Enum<*>>(x3)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Enum<*>")!>x3<!>
}

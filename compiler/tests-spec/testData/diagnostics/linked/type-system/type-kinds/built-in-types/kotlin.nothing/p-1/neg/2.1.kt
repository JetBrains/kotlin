// !LANGUAGE: +NewInference
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: type-system, type-kinds, built-in-types, kotlin.nothing -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Check of Nothing type
 * HELPERS: checkType
 */


// TESTCASE NUMBER: 1
class Case1(val nothing: Nothing)

fun case1() {
    val res = Case1(Nothing())
}
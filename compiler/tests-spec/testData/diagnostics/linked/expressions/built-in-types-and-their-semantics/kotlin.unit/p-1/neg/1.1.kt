// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: expressions, built-in-types-and-their-semantics, kotlin.unit -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check kotlin.Unit is a unit type
 * HELPERS: checkType
 */


// TESTCASE NUMBER: 1

fun case1() {
    val unitInherited: UnitInherited = UnitInherited()
}

class UnitInherited : <!SINGLETON_IN_SUPERTYPE!>Unit<!> {}


// TESTCASE NUMBER: 2

fun case2() {
    val p2 = object : <!SINGLETON_IN_SUPERTYPE!>kotlin.Unit<!> {
        override fun toString() = "inherited.Unit"
    }
}

// TESTCASE NUMBER: 3

fun case3() {
    val aInherited: A<<!UPPER_BOUND_VIOLATED!>UnitInherited<!>> = A<<!UPPER_BOUND_VIOLATED!>UnitInherited<!>>()
    val aOriginal: A<Unit> = A<Unit>()

    val bInherited: B<<!UPPER_BOUND_VIOLATED!>UnitInherited<!>, UnitInherited> = B<<!UPPER_BOUND_VIOLATED!>UnitInherited<!>, UnitInherited>()
    val bOriginal: B<Unit, Unit> = B<Unit, Unit>()
}

class A<T : <!FINAL_UPPER_BOUND!>Unit<!>> {}
class B<T, K> where T : <!FINAL_UPPER_BOUND!>Unit<!>, K: Any{}

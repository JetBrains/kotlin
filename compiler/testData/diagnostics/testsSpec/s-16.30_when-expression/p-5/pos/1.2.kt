// !CHECK_TYPE

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 5
 SENTENCE 1: The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 2
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via enum).
 */

enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

open class A {}
open class B: A() {}
open class C: B() {}
open class D: C() {}
open class E: D() {}
class F: E() {}

// CASE DESCRIPTION: Checking correct type in 'when'.
fun case_1(value: Direction): String {
    val whenValue = when (value) {
        Direction.EAST -> B()
        Direction.NORTH -> C()
        Direction.SOUTH -> D()
        Direction.WEST -> E()
    }

    whenValue checkType { _<B>() }
    checkSubtype<A>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct type in 'when' with null-check branch.
fun case_2(value: Direction?): String {
    val whenValue = when (value) {
        Direction.EAST -> B()
        Direction.NORTH -> C()
        Direction.SOUTH -> D()
        Direction.WEST -> E()
        null -> F()
    }

    whenValue checkType { _<B>() }
    checkSubtype<A>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when'.
fun case_3(value: Direction): String {
    val whenValue = when (value) {
        Direction.EAST -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        Direction.NORTH -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        Direction.SOUTH -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        Direction.WEST -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' with null-check branch.
fun case_4(value: Direction?): String {
    val whenValue = when (value) {
        Direction.EAST -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        Direction.NORTH -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        Direction.SOUTH -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
        Direction.WEST -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
        null -> <!IMPLICIT_CAST_TO_ANY!>false<!>
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

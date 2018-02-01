// WITH_RUNTIME

import kotlin.test.assertEquals

class Equlitive(val value: Int) {
    operator fun component1() = value

    infix fun eq(other: Equalitive) = Equlitive(value + other.value)
    
    infix fun eq(other: Int) = value + other + 21

    override fun equals(other: Any?) = when (other) {
        is Equlitive(val o) -> o == value
        is val o: Int -> o == value + 5
        else -> false
    }
}

fun foo(a: Equlitive, eq: Int) = when (a) {
    is (eq) -> 1
    is (eq eq) -> 2
    is eq eq -> 3
    !is (eq) -> 4
    !is (eq eq) -> 5
    !is eq eq -> 6
    else -> 0
}

fun foo2(a: Equlitive, eq: Int) = when (a) {
    is (eq eq) -> 2
    is (eq) -> 1
    is eq eq -> 3
    !is (eq eq) -> 5
    !is (eq) -> 4
    !is eq eq -> 6
    else -> 0
}

fun foo3(a: Equlitive, eq: Int, _eq: Equlitive) = when (a) {
    is (eq) -> 1
    is (_eq) -> 2
    is (_eq eq eq) -> 5
    is (_eq eq _eq) -> 6

    is (eq eq) -> 3
    is (eq _eq) -> 4
    is (eq _eq eq eq) -> 7
    is (eq _eq eq _eq) -> 8

    is eq eq -> 13
    is eq _eq -> 14
    is eq _eq eq eq -> 17
    is eq _eq eq _eq -> 18

    !is (eq) -> -1
    !is (_eq) -> -2
    !is (_eq eq eq) -> -5
    !is (_eq eq _eq) -> -6

    !is (eq eq) -> -3
    !is (eq _eq) -> -4
    !is (eq _eq eq eq) -> -7
    !is (eq _eq eq _eq) -> -8

    !is eq eq -> -13
    !is eq _eq -> -14
    !is eq _eq eq eq -> -17
    !is eq _eq eq _eq -> -18
    else -> 0
}

fun foo4(a: Equlitive, eq: Int, _eq: Equlitive) = when (a) {
    is (eq eq) -> 3
    is (eq _eq) -> 4
    is (eq _eq eq eq) -> 7
    is (eq _eq eq _eq) -> 8

    is (eq) -> 1
    is (_eq) -> 2
    is (_eq eq eq) -> 5
    is (_eq eq _eq) -> 6

    is eq eq -> 13
    is eq _eq -> 14
    is eq _eq eq eq -> 17
    is eq _eq eq _eq -> 18

    !is (eq eq) -> -3
    !is (eq _eq) -> -4
    !is (eq _eq eq eq) -> -7
    !is (eq _eq eq _eq) -> -8

    !is (eq) -> -1
    !is (_eq) -> -2
    !is (_eq eq eq) -> -5
    !is (_eq eq _eq) -> -6

    !is eq eq -> -13
    !is eq _eq -> -14
    !is eq _eq eq eq -> -17
    !is eq _eq eq _eq -> -18
    else -> 0
}

fun foo5(a: Equlitive, eq: Int, _eq: Equlitive) = when (a) {
    is eq eq -> 13
    is eq _eq -> 14
    is eq _eq eq eq -> 17
    is eq _eq eq _eq -> 18

    is (eq eq) -> 3
    is (eq _eq) -> 4
    is (eq _eq eq eq) -> 7
    is (eq _eq eq _eq) -> 8

    is (eq) -> 1
    is (_eq) -> 2
    is (_eq eq eq) -> 5
    is (_eq eq _eq) -> 6

    !is eq eq -> -13
    !is eq _eq -> -14
    !is eq _eq eq eq -> -17
    !is eq _eq eq _eq -> -18

    !is (eq eq) -> -3
    !is (eq _eq) -> -4
    !is (eq _eq eq eq) -> -7
    !is (eq _eq eq _eq) -> -8

    !is (eq) -> -1
    !is (_eq) -> -2
    !is (_eq eq eq) -> -5
    !is (_eq eq _eq) -> -6
    else -> 0
}

fun box(): String {
    assertEquals(foo(Equlitive(1), 1), 1)
    assertEquals(foo(Equlitive(1), 6), 3)
    assertEquals(foo(Equlitive(1), 3), 4)

    assertEquals(foo2(Equlitive(1), 1), 2)
    assertEquals(foo2(Equlitive(1), 6), 3)
    assertEquals(foo2(Equlitive(1), 3), 5)

    assertEquals(foo3(Equlitive(1), 1, Equlitive(1)), 1)
    assertEquals(foo3(Equlitive(6), 1, Equlitive(1)), 2)
    assertEquals(foo3(Equlitive(23), 1, Equlitive(1)), 5)
    assertEquals(foo3(Equlitive(2), 1, Equlitive(1)), 6)
    assertEquals(foo3(Equlitive(28), 1, Equlitive(1)), -1)

    assertEquals(foo4(Equlitive(1), 1, Equlitive(1)), 3)
    assertEquals(foo4(Equlitive(6), 1, Equlitive(1)), 4)
    assertEquals(foo4(Equlitive(23), 1, Equlitive(1)), 7)
    assertEquals(foo4(Equlitive(2), 1, Equlitive(1)), 8)
    assertEquals(foo4(Equlitive(28), 1, Equlitive(1)), -3)

    assertEquals(foo5(Equlitive(6), 1, Equlitive(1)), 13)
    assertEquals(foo5(Equlitive(1), 1, Equlitive(1)), 14)
    assertEquals(foo5(Equlitive(28), 1, Equlitive(1)), 17)
    assertEquals(foo5(Equlitive(2), 1, Equlitive(1)), 18)
    assertEquals(foo5(Equlitive(23), 1, Equlitive(1)), -13)
}

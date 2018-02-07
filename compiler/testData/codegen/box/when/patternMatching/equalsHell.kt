// WITH_RUNTIME

import kotlin.test.assertEquals

class IntWrapper(val value: Int) {
    operator fun component1() = value

    operator fun plus(other: IntWrapper) = value + other.value

    operator fun plus(other: Int) = value + other

    override fun equals(other: Any?) = when (other) {
        is Equlitive -> other == value
        is IntWrapper(val o) -> value == o
        else -> value == other
    }
}

class Equlitive(val value: IntWrapper) {
    constructor(value: Int) : this(IntWrapper(value))

    operator fun component1() = value

    infix fun eq(other: Equlitive) = Equlitive(value + other.value)

    infix fun eq(other: IntWrapper) = value + other + 21

    infix fun eq(other: Int) = value + other + 21

    override fun equals(other: Any?) = when (other) {
        is Equlitive(val o) -> o == value
        is IntWrapper(val o) -> o == value + 5
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
    assertEquals(1, foo(Equlitive(1), 1))
    assertEquals(3, foo(Equlitive(1), 6))
    assertEquals(4, foo(Equlitive(1), 3))

    assertEquals(2, foo2(Equlitive(1), 1))
    assertEquals(3, foo2(Equlitive(1), 6))
    assertEquals(5, foo2(Equlitive(1), 3))

    assertEquals(1, foo3(Equlitive(1), 1, Equlitive(1)))
    assertEquals(2, foo3(Equlitive(6), 1, Equlitive(1)))
    assertEquals(5, foo3(Equlitive(23), 1, Equlitive(1)))
    assertEquals(6, foo3(Equlitive(7), 1, Equlitive(1)))
    assertEquals(18, foo3(Equlitive(2), 1, Equlitive(1)))
    assertEquals(-1, foo3(Equlitive(28), 1, Equlitive(1)))

    assertEquals(3, foo4(Equlitive(1), 1, Equlitive(1)))
    assertEquals(4, foo4(Equlitive(6), 1, Equlitive(1)))
    assertEquals(7, foo4(Equlitive(23), 1, Equlitive(1)))
    assertEquals(8, foo4(Equlitive(7), 1, Equlitive(1)))
    assertEquals(18, foo4(Equlitive(2), 1, Equlitive(1)))
    assertEquals(-3, foo4(Equlitive(28), 1, Equlitive(1)))

    assertEquals(13, foo5(Equlitive(1), 6, Equlitive(1)))
    assertEquals(4, foo5(Equlitive(6), 1, Equlitive(1)))
    assertEquals(14, foo5(Equlitive(1), 1, Equlitive(1)))
    assertEquals(17, foo5(Equlitive(18), 1, Equlitive(1)))
    assertEquals(18, foo5(Equlitive(2), 1, Equlitive(1)))
    assertEquals(7, foo5(Equlitive(23), 1, Equlitive(1)))
    assertEquals(-13, foo5(Equlitive(28), 1, Equlitive(1)))

    return "OK"
}

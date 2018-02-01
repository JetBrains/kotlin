class Equlitive(val value: Int) {
    operator fun component1() = value

    infix fun eq(other: Equalitive) = Equlitive(value + other.value)
    
    infix fun eq(other: Int) = value + other.value

    override fun equals(other: Any?) = when (other) {
        is Equlitive(val o) -> o == value
        is val o: Int -> o == value + 1
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

fun foo2(a: Equlitive) = when (a) {
    is (<!UNRESOLVED_REFERENCE!>eq<!>) -> 1
    is (eq <!UNRESOLVED_REFERENCE!>eq<!>) -> 2
    is eq <!UNRESOLVED_REFERENCE!>eq<!> -> 3
    !is (<!UNRESOLVED_REFERENCE!>eq<!>) -> 4
    !is (eq <!UNRESOLVED_REFERENCE!>eq<!>) -> 5
    !is eq <!UNRESOLVED_REFERENCE!>eq<!> -> 6
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

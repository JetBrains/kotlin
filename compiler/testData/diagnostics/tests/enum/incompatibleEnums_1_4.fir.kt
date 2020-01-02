// !LANGUAGE: +ProhibitComparisonOfIncompatibleEnums

enum class E1 {
    A, B
}

enum class E2 {
    A, B
}

fun foo1(e1: E1, e2: E2) {
    e1 == e2
    e1 != e2

    e1 == E2.A
    E1.B == e2

    E1.A == E2.B

    e1 == E1.A
    E1.A == e1
    e2 == E2.B
    E2.B == e2
}

fun foo2(e1: E1, e2: E2) {
    when (e1) {
        E1.A -> {}
            E2.A -> {}
            E2.B -> {}
        e1 -> {}
            e2 -> {}
        else -> {}
    }
}

fun foo3(e1: Enum<E1>, e2: Enum<E2>, e: Enum<*>) {
    e1 == e
    e1 == e2

    e1 == E1.A
    e1 == E2.A

    when (e1) {
        e1 -> {}
        e2 -> {}
        e -> {}
        E1.A -> {}
            E2.A -> {}
        else -> {}
    }

    when (e) {
        e -> {}
        e2 -> {}
        E1.A -> {}
        E2.A -> {}
        else -> {}
    }
}

interface MyInterface
open class MyOpenClass

fun foo4(e1: E1, i: MyInterface, c: MyOpenClass) {
    e1 == i
    i == e1

    e1 == c
    c == e1

    when (e1) {
            i -> {}
            c -> {}
        else -> {}
    }
}

enum class E3 : MyInterface { X, Y }

fun foo5(i: MyInterface, a: Any) {
    E3.X == E3.Y
    E3.X == i
    E3.X == a
}

fun foo6(e1: E1?, e2: E2) {
    E1.A == null
    null == E1.A
    e1 == null
    null == e1

    e1 == E2.A
    E2.A == e1
    e1 == e2
    e2 == e1

    e2 == null
    null == e2
    E1.A == null
    null == E1.A
}

fun foo7(e1: E1?, e2: E2?) {
    e1 == e2 // There should be an IDE-inspection for such cases
}

fun <T> foo8(e1: E1?, e2: E2, t: T) {
    e1 == t
    t == e1

    e2 == t
    t == e2

    E1.A == t
    t == E1.A
}

fun <T, K> foo9(e1: E1?, e2: E2, t: T, k: K) where T : MyInterface, T : MyOpenClass, K : MyInterface {
    e1 == t
    t == e1

    e2 == t
    t == e2

    E1.A == t
    t == E1.A

    E3.X == t

    E3.X == k
    k == E3.X
}

interface Inv<T>

enum class E4 : Inv<Int> { A }

fun foo10(e4: E4, invString: Inv<String>) {
    e4 == invString
    invString == e4

    E4.A == invString
    invString == E4.A
}
// WITH_STDLIB
// ISSUE: KT-61133

interface A

fun xxx() {
    class B(val x: Long) : A
    class C(val b1: A, val b2: A) : A

    listOf(1L, 2L)
        // We cannot remove 'as A', otherwise we get TYPE_MISMATCH in reduce
        .map { B(it) as A }
        .reduce { a, b -> C(a, b) }
}

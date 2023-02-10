// WITH_STDLIB
// ISSUE: KT-47750

sealed interface I1 {
    interface B : I1
    interface C : I1
}

sealed interface I2 {
    interface B : I2
    interface C : I2
}

fun test_1(x: I1) {
    val a = when (x) {
        is I1.B -> 1
        is I1.C -> 1
    }
    require(x is I2)
    val b = when (x) {
        is I1.B -> 1
        is I1.C -> 1
    }

    val c = when (x) {
        is I2.B -> 1
        is I2.C -> 1
    }
}

fun test_2(x: Any) {
    require(x is I1)
    val a = when (x) {
        is I1.B -> 1
        is I1.C -> 1
    }
    require(x is I2)
    val b = when (x) {
        is I1.B -> 1
        is I1.C -> 1
    }
    val c = when (x) {
        is I2.B -> 1
        is I2.C -> 1
    }
}

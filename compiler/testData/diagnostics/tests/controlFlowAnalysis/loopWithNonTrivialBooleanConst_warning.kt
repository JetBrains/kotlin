// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// DIAGNOSTICS: -UNUSED_VARIABLE

fun test_1() {
    while (true) {

    }
    <!UNREACHABLE_CODE!>val x = 1<!>
}

fun test_2() {
    while (<!NON_TRIVIAL_BOOLEAN_CONSTANT!>true || false<!>) {

    }
    <!UNREACHABLE_CODE!>val x = 1<!>
}

fun test_3() {
    while (<!NON_TRIVIAL_BOOLEAN_CONSTANT!>1 == 1<!>) {

    }
    <!UNREACHABLE_CODE!>val x = 1<!>
}

fun test_4() {
    while (false) {
        val x = 1
    }
    val y = 2
}

fun test_5() {
    while (<!NON_TRIVIAL_BOOLEAN_CONSTANT!>false && true<!>) {
        val x = 1
    }
    val y = 2
}

fun test_6() {
    do {

    } while (true)
    <!UNREACHABLE_CODE!>val x = 1<!>
}

fun test_7() {
    do {

    } while (<!NON_TRIVIAL_BOOLEAN_CONSTANT!>true || false<!>)
    <!UNREACHABLE_CODE!>val x = 1<!>
}

fun test_8() {
    do {

    } while (<!NON_TRIVIAL_BOOLEAN_CONSTANT!>1 == 1<!>)
    <!UNREACHABLE_CODE!>val x = 1<!>
}

fun test_9() {
    do {
        val x = 1
    } while (false)
    val y = 2
}

fun test_10() {
    do {
        val x = 1
    } while (<!NON_TRIVIAL_BOOLEAN_CONSTANT!>false && true<!>)
    val y = 2
}

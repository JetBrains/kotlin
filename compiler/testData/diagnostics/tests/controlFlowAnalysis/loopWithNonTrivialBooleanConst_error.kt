// LANGUAGE: +ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// DIAGNOSTICS: -UNUSED_VARIABLE

fun test_1() {
    while (true) {

    }
    <!UNREACHABLE_CODE!>val x = 1<!>
}

fun test_2() {
    while (true || false) {

    }
    val x = 1
}

fun test_3() {
    while (1 == 1) {

    }
    val x = 1
}

fun test_4() {
    while (false) {
        val x = 1
    }
    val y = 2
}

fun test_5() {
    while (false && true) {
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

    } while (true || false)
    val x = 1
}

fun test_8() {
    do {

    } while (1 == 1)
    val x = 1
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
    } while (false && true)
    val y = 2
}


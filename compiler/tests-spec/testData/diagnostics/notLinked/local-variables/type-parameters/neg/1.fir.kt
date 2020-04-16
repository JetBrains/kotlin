// !DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: local-variables, type-parameters
 * NUMBER: 1
 * DESCRIPTION: Local variables with forbidden type parameters.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-8341
 */

// TESTCASE NUMBER: 1
fun case_1() {
    val <T> x1 = 1
    var <T> x2 = 1
}

// TESTCASE NUMBER: 2
fun case_2() {
    val <T> x1: Int = 1
    var <T> x2: Int = 1
}

// TESTCASE NUMBER: 3
fun case_3() {
    val <T> x1: Map<Int, Int> = mapOf(1 to 1)
    var <T> x2: Map<Int, Int> = mapOf(1 to 1)
}

// TESTCASE NUMBER: 4
fun case_4() {
    val <T> y1: Number where __UNRESOLVED__: __UNRESOLVED__ = 1
    var <T> y2: Number where __UNRESOLVED__: __UNRESOLVED__ = 1
}

// TESTCASE NUMBER: 5
fun case_5() {
    val <T : __UNRESOLVED__> x1: Map<Int, Int> = mapOf(1 to 1)
    var <T : __UNRESOLVED__> x2: Map<Int, Int> = mapOf(1 to 1)
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 */
fun case_6() {
    val <T : __UNRESOLVED__> (x1, y1) = Pair(1, 2)
    var <T : __UNRESOLVED__> (x2, y2) = Pair(1, 2)
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 */
fun case_7() {
    val <T : __UNRESOLVED__> (x1, y1) where __UNRESOLVED__: __UNRESOLVED__ = Pair(1, 2)
    var <T : __UNRESOLVED__> (x2, y2) where __UNRESOLVED__: __UNRESOLVED__ = Pair(1, 2)
}

// TESTCASE NUMBER: 8
fun case_8() {
    val <A, B : A, C : B, D : C, E : D> x1 = 1
    var <A, B : A, C : B, D : C, E : D> x2 = 2
}

// TESTCASE NUMBER: 9
fun case_9(y: Boolean?) = when (val <T> x = y) {
    true -> null
    false -> null
    null -> null
}

// TESTCASE NUMBER: 10
fun case_10(x: Boolean?) = when (val <T> x where T: suspend () -> Unit, T: Boolean = x) {
    true -> null
    false -> null
    null -> null
}

// TESTCASE NUMBER: 11
fun case_11() {
    val <T> x by lazy { 1 }
    var <T> x by lazy { 1 }
}

// TESTCASE NUMBER: 12
fun case_12() {
    val <T : __UNRESOLVED__> x: Int
    var <T : __UNRESOLVED__> x: Int
}

// TESTCASE NUMBER: 13
fun case_13() {
    val <T : __UNRESOLVED__> x: Int where __UNRESOLVED__: __UNRESOLVED__
    var <T : __UNRESOLVED__> x: Int where __UNRESOLVED__: __UNRESOLVED__
}

// TESTCASE NUMBER: 14
fun case_14() {
    val <T : T> x1 = 1
    var <T : T> x2 = 1
}

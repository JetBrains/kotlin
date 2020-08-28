// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
fun case_1(value_1: SealedClass): String = when(value_1) {
    is SealedChild1 -> ""
    is SealedChild2 -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: SealedClass): String = when(value_1) {
    is SealedChild1, is SealedChild2 -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: SealedClassMixed): String = when(value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2 -> ""
    SealedMixedChildObject1 -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: SealedClassMixed): String = when(value_1) {
    SealedMixedChildObject1, is SealedMixedChild2, is SealedMixedChild1 -> ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: SealedClassMixed): String = when(value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2 -> ""
    is SealedMixedChild3 -> ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: SealedClassMixed): Int = when(value_1) { }

// TESTCASE NUMBER: 7
fun case_7(value_1: SealedClassSingleWithObject): Int = when(value_1) { }

// TESTCASE NUMBER: 8
fun case_8(value_1: SealedClassEmpty): String = when (value_1) { }

// TESTCASE NUMBER: 9
fun case_9(value_1: Number): String = when (value_1) {
    is Byte -> ""
    is Double -> ""
    is Float -> ""
    is Int -> ""
    is Long -> ""
    is Short -> ""
}

/*
 * TESTCASE NUMBER: 10
 * DISCUSSION: maybe make exhaustive without else?
 */
fun case_10(value_1: Any): String = when (value_1) {
    is Any -> ""
}

// TESTCASE NUMBER: 11
fun case_11(value_1: SealedClass): String = when {
    value_1 is SealedChild1 -> ""
    value_1 is SealedChild2 -> ""
    value_1 is SealedChild3 -> ""
}

// TESTCASE NUMBER: 12
fun case_12(value_1: SealedClassMixed): String = when(value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2 -> ""
    is SealedMixedChild3 -> ""
}

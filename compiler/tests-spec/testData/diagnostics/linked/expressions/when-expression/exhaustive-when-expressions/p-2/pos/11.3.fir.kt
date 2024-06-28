// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 11
 * NUMBER: 3
 * DESCRIPTION: Exhaustive when using subclasses of the nullable sealed class.
 * HELPERS: sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: SealedClass?): Int = when (value_1) {
    is SealedChild1 -> value_1.number
    is SealedChild2 -> value_1.e1 + value_1.e2
    is SealedChild3 -> value_1.m1 + value_1.m2
    null -> 0
}

// TESTCASE NUMBER: 2
fun case_2(value_1: SealedClass?): String = when (value_1) {
    is SealedClass -> ""
    null -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: SealedClassWithMethods?): String = when (value_1) {
    is SealedWithMethodsChild1 -> value_1.m1()
    is SealedWithMethodsChild2 -> value_1.m2()
    is SealedWithMethodsChild3 -> value_1.m3()
    null -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: SealedClassWithObjects?): String = when (value_1) {
    SealedWithObjectsChild1 -> ""
    SealedWithObjectsChild2 -> ""
    SealedWithObjectsChild3 -> ""
    null -> ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: SealedClassMixed?): String = when (value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2 -> ""
    is SealedMixedChild3 -> ""
    SealedMixedChildObject1 -> ""
    SealedMixedChildObject2 -> ""
    SealedMixedChildObject3 -> ""
    null -> ""
}

/*
 * TESTCASE NUMBER: 6
 * DISCUSSION: is it correct that objects can be checked using the type checking operator?
 */
fun case_6(value_1: SealedClassMixed?): String = when (value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2 -> ""
    is SealedMixedChild3 -> ""
    is SealedMixedChildObject1 -> ""
    is SealedMixedChildObject2 -> ""
    is SealedMixedChildObject3 -> ""
    null -> ""
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR: must be exhaustive
 * ISSUES: KT-26044
 */
fun case_7(value: SealedClassEmpty?): String = when (value) {
    null -> ""
}

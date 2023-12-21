// !LANGUAGE: +ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-313
 * MAIN LINK: expressions, when-expression -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION:  it is possible to  replace the else condition with an always-true condition
 */

// FILE: JavaEnum.java

enum JavaEnum {
    Val_1,
    Val_2,
}

// FILE: KotlinClass.kt

// TESTCASE NUMBER: 1
fun case1() {
    val z = JavaEnum.Val_1
    val when2 = <!NO_ELSE_IN_WHEN!>when<!> (z) {
        JavaEnum.Val_1 -> { }
        <!DUPLICATE_BRANCH_CONDITION_IN_WHEN!>JavaEnum.Val_1<!> -> { }
    }

}

// TESTCASE NUMBER: 2

fun case2() {
    val b = false
    val when2: Any = <!NO_ELSE_IN_WHEN!>when<!> (b) {
        false -> { }
        <!DUPLICATE_BRANCH_CONDITION_IN_WHEN!>false<!> -> { }
    }
}

// TESTCASE NUMBER: 3

fun case3() {
    val a = false
    val when2: Any = <!NO_ELSE_IN_WHEN!>when<!> (a) {
        true -> { }
        <!DUPLICATE_BRANCH_CONDITION_IN_WHEN!>true<!> -> { }
    }
}

// TESTCASE NUMBER: 4

fun case4() {
    val x: SClass = SClass.B()
    val when2 = <!NO_ELSE_IN_WHEN!>when<!> (x){
        is  SClass.A ->{ }
        is  SClass.B ->{ }
        is  <!DUPLICATE_BRANCH_CONDITION_IN_WHEN!>SClass.B<!> ->{ }
    }
}

sealed class SClass {
    class A : SClass()
    class B : SClass()
    class C : SClass()
}

// TESTCASE NUMBER: 5

fun case5() {
    val b = false
    val when1: Any = when (b) {
        false -> { }
        !false -> { }
            else -> { }
    }

    val when2: Any = <!NO_ELSE_IN_WHEN!>when<!> (b) {
        false -> { }
        !false -> { }
    }
    val when3: Any = <!NO_ELSE_IN_WHEN!>when<!> (b) {
        false -> { }
            <!DUPLICATE_BRANCH_CONDITION_IN_WHEN!>false<!> -> { }
        !false -> { }
    }
}

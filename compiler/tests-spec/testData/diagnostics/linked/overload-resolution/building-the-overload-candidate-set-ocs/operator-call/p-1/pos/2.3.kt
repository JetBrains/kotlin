// FIR_IDENTICAL
// !DIAGNOSTICS: -UNNECESSARY_SAFE_CALL -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-448
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 1 -> sentence 2
 * SECONDARY LINKS: statements, assignments, operator-assignments -> paragraph 2 -> sentence 1
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 2
 * statements, assignments, operator-assignments -> paragraph 2 -> sentence 3
 * NUMBER: 3
 * DESCRIPTION: Non-extension member callables
 */

// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1

fun case1(a: A, c: C) {
    a.b.<!DEBUG_INFO_CALL("fqName: testPackCase1.B.plusAssign; typeCall: operator function")!>plusAssign(c)<!>

    val x = {
        a.b.<!DEBUG_INFO_CALL("fqName: testPackCase1.B.plusAssign; typeCall: operator function")!>plusAssign(c)<!>
    }()

    a.b.<!DEBUG_INFO_CALL("fqName: testPackCase1.B.plusAssign; typeCall: operator function")!>plusAssign({ c }())<!>
}

class A(val b: B)

class B {
    operator fun plusAssign(c: C) {
        print("1")
    }

    operator fun plus(c: C): C {
        print("2")
        return c
    }
}

class C

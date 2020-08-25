// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: declarations, inheritance
 * NUMBER: 1
 * DESCRIPTION: property merging
 */

// TESTCASE NUMBER: 1
// UNEXPECTED BEHAVIOUR
// ISSUES: KT-41222

fun case1() {
    val c = Case1()
    print(c.boo)
    c.boo = 1
    print(c.boo)
}

interface I {
    var boo: Int
}

abstract class A() {
    val boo: Int = 2
}

class Case1 : I, A()

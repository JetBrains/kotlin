// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: declarations, enum-class-declaration
 * NUMBER: 2
 * DESCRIPTION: override of name when enum inherits an interface with variable ordinal
 */

// TESTCASE NUMBER: 1
// UNEXPECTED BEHAVIOUR
// ISSUES: KT-41165

fun case1(): String {
    print(Case1.VAL1.ordinal)
    Case1.VAL1.ordinal = 1
    print(Case1.VAL1.ordinal)
    return "NOK"
}

interface I {
    var ordinal: Int
}

private enum class Case1 : I {
    VAL1, VAL2;
}
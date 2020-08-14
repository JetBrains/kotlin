// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: declarations, enum-class-declaration
 * NUMBER: 1
 * DESCRIPTION: override of name when enum inherits an interface with variable ordinal
 */

// TESTCASE NUMBER: 1
// UNEXPECTED BEHAVIOUR
// ISSUES: KT-41165

fun case1(): String {
    print(Case1.VAL1.name)
    Case1.VAL1.name = "boo"
    print(Case1.VAL1.name)
    return "NOK"
}

interface I{
    public var name: String
}

enum class Case1 : I {
    VAL1, VAL2;
}
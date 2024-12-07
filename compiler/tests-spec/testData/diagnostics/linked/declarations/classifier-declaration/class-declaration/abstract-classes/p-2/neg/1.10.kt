// FIR_IDENTICAL
// LANGUAGE: +ProhibitInvisibleAbstractMethodsInSuperclasses
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT
// FULL_JDK

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * MAIN LINK: declarations, classifier-declaration, class-declaration, abstract-classes -> paragraph 2 -> sentence 1
 * PRIMARY LINKS: inheritance, overriding -> paragraph 7 -> sentence 1
 * NUMBER: 10
 * DESCRIPTION: Abstract classes may contain one or more abstract members, which should be implemented in a subtype of this abstract class
 * ISSUES: KT-27825
 */

// TESTCASE NUMBER: 1
<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class Case1<!>() : BaseCase1(), InterfaceCase1 {}

abstract class BaseCase1() {
    abstract fun foo(): String
    abstract val a: String
}

interface InterfaceCase1 {
    fun foo() = "foo"
    val a: String
        get() = "a"
}

// TESTCASE NUMBER: 2
class Case2Outer {
    val v = "v"

    abstract class Case2Base() {
        abstract fun foo(): String
    }

    inner
    <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class A<!>() : Case2Base() {

    }
}

// TESTCASE NUMBER: 3
fun case3() {
    <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>object<!> : CaseOuter.CaseBase() {}.outerFoo()
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class B<!>() : CaseOuter.CaseBase() {}

sealed class CaseOuter {
    val v = "v"
    abstract fun outerFoo();

    abstract class CaseBase() : CaseOuter() {
        abstract fun foo(): String
    }

    <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class A<!>() : CaseBase() {
    }
}
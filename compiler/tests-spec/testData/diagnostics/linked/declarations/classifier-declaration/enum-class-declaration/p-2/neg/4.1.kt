// !LANGUAGE: +NewInference
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, enum-class-declaration -> paragraph 2 -> sentence 4
 * PRIMARY LINKS: declarations, classifier-declaration, enum-class-declaration -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: enum class is implicitly final and cannot be inherited from

 */
// TESTCASE NUMBER: 1
<!WRONG_MODIFIER_TARGET!>open<!> enum class Case1()
class C1 : <!INVISIBLE_MEMBER!>Case1<!>()

// TESTCASE NUMBER: 2
<!WRONG_MODIFIER_TARGET!>open<!> enum class Case2
class Case() {
    class C2 : <!INVISIBLE_MEMBER!>Case2<!>()
}

// TESTCASE NUMBER: 3
<!WRONG_MODIFIER_TARGET!>abstract<!> enum class Case3
class C3 : <!INVISIBLE_MEMBER!>Case3<!>()

// TESTCASE NUMBER: 4
enum class Case4()
class C4 : <!FINAL_SUPERTYPE, INVISIBLE_MEMBER!>Case4<!>()

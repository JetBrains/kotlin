// !LANGUAGE: +NewInference
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, enum-class-declaration -> paragraph 2 -> sentence 4
 * PRIMARY LINKS: declarations, classifier-declaration, enum-class-declaration -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: enum class is implicitly final and cannot be inherited from

 */
// TESTCASE NUMBER: 1
final enum class Case1()

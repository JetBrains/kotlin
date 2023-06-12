// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-544
 * MAIN LINK: declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 4 -> sentence 1
 * PRIMARY LINKS: declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 5 -> sentence 1
 * declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 5 -> sentence 2
 * declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 5 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: primary constructor with regular, read-only and mutable parameters at once
 */

// TESTCASE NUMBER: 1
class C1(x1: Boolean, val x2: Boolean, var x3: Boolean)

// TESTCASE NUMBER: 2
class C2(x1: Boolean, var x2: Boolean, vararg var x3: Boolean)

// TESTCASE NUMBER: 3
class C3(x1: Boolean, vararg val x2: Boolean, var x3: Boolean)

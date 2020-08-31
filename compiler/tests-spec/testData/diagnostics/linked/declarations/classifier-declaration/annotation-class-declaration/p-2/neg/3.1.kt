// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, annotation-class-declaration -> paragraph 2 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: cannot have any other base classes exept kotlin.Annotation
 */

// TESTCASE NUMBER: 1
annotation class Case1() : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>OC()<!>

abstract class OC

// TESTCASE NUMBER: 2
annotation class Case2() : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>C.OC()<!>

class C {
    open class OC
}

// TESTCASE NUMBER: 3
annotation class Case3() : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>SC()<!>

sealed class SC


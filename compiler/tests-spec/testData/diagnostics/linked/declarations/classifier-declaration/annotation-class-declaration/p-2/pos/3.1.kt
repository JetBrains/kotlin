// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, annotation-class-declaration -> paragraph 2 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION:  implicitly inherit kotlin.Annotation
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
import kotlin.reflect.full.createInstance

annotation class Case1(val why: String)

fun case1(){
    val annotation = Case1::class.createInstance()

    checkSubtype<kotlin.Annotation>(annotation)
}
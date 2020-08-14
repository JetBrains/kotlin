// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, classifier-initialization -> paragraph 8 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: If any step in the initialization order creates a loop, it is considered to be undefined behaviour
 */
// TESTCASE NUMBER: 1

open class Case1(var x: Int) {

    object VAL1 : Case1(1) {
        init {
            println("init VAR1")
        }
    }

    object VAL2 : Case1(2) {
        init {
            println("init VAR2")
        }
    }

    init {
        println("init Case1")
        try {
            VAL1.x // NPE
        } catch (e: NullPointerException) {
            println("NPE")
        }
    }
}
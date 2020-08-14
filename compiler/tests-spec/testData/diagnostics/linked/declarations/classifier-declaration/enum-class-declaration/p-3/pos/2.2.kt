// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, enum-class-declaration -> paragraph 3 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: body with init block
 */

// TESTCASE NUMBER: 1

enum class Case1(var x: Int) {

    VAL1(1) {
        init {

        }
    },
    VAL2(2) {
        init {
            println("First initializer block that prints ${name}")
            VAL1.x = 3
            print(VAL1.x)
        }
    };

}
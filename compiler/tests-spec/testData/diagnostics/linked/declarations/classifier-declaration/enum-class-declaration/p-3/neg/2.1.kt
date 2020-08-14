// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, enum-class-declaration -> paragraph 3 -> sentence 2
 * PRIMARY LINKS: declarations, classifier-declaration, classifier-initialization -> paragraph 9 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: body with init block
 */

// TESTCASE NUMBER: 1

enum class Case1(var x: Int) {

    VAL1(1) {
        init {
            println(" initializer block that prints ${name}")
            <!UNINITIALIZED_ENUM_ENTRY!>VAL2<!>.x = 4
            print(VAL2.x)
        }
    },
    VAL2(2) {

    };

}
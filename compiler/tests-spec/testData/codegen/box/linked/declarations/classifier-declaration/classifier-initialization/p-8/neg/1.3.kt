// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, classifier-initialization -> paragraph 8 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: entry initialization an enum class
 * EXCEPTION: compiletime
 */

fun box(): String {
    Case1.VAL1
    return "NOK"
}

enum class Case1 {
    VAL1,
    VAL2;

    init {
        VAL1 // uninitialized enum entry
    }
}


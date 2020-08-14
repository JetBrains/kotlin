// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, classifier-initialization -> paragraph 8 -> sentence 1
 * PRIMARY LINKS: declarations, classifier-declaration, classifier-initialization -> paragraph 9 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: entry initialization an enum class with a property call
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41124
 * EXCEPTION: runtime
 */

fun box(): String {
    Case1.VAL1
    return "NOK"
}

enum class Case1(var x: Int) {
    VAL1(1),
    VAL2(2);

    init {
        Case1.VAL1.x // NPE
    }
}

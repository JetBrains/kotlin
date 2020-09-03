// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 9
 * PRIMARY LINKS: type-inference, smart-casts, smart-cast-types -> paragraph 9 -> sentence 8
 * NUMBER: 1
 * DESCRIPTION:
 * EXCEPTION: compiletime
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-41575, 41652
 */

fun box(l: Any?) {
    when (l as Any) {
        is String -> {
            val y: String = l //type mismatch wrong msg
        }
    }
}
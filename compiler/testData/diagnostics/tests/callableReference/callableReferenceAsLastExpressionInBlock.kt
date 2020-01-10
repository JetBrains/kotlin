// !DIAGNOSTICS: -UNUSED_VARIABLE
// !CHECK_TYPE
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-220, test type: pos):
 *  - expressions, call-and-property-access-expressions, callable-references -> paragraph 11 -> sentence 3
 */
import kotlin.reflect.KFunction0

fun test() {
    val a = if (true) {
        val x = 1
        "".length
        ::foo
    } else {
        ::foo
    }
    a checkType {  _<KFunction0<Int>>() }
}

fun foo(): Int = 0
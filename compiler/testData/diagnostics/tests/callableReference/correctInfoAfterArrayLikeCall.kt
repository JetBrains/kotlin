// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-220, test type: pos):
 *  - expressions, call-and-property-access-expressions, navigation-operators -> paragraph 9 -> sentence 2
 *  - expressions, call-and-property-access-expressions, navigation-operators -> paragraph 8 -> sentence 1
 */
import kotlin.reflect.KProperty1

class DTO {
    val q: Int = 0
    operator fun get(prop: KProperty1<*, Int>): Int = 0
}

fun foo(intDTO: DTO?, p: KProperty1<*, Int>) {
    if (intDTO != null) {
        <!DEBUG_INFO_SMARTCAST!>intDTO<!>[DTO::q]
        <!DEBUG_INFO_SMARTCAST!>intDTO<!>.q
    }
}

// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

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

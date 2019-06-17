// !LANGUAGE: +NewInference

class DTO {
    val q: Int = 0
    operator fun get(<!UNUSED_PARAMETER!>prop<!>: <!UNRESOLVED_REFERENCE!>KProperty1<!><*, Int>): Int = 0
}

fun foo(intDTO: DTO?, <!UNUSED_PARAMETER!>p<!>: <!UNRESOLVED_REFERENCE!>KProperty1<!><*, Int>) {
    if (intDTO != null) {
        <!DEBUG_INFO_SMARTCAST!>intDTO<!>[DTO::q]
        <!DEBUG_INFO_SMARTCAST!>intDTO<!>.q
    }
}

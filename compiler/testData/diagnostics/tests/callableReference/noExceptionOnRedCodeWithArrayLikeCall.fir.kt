
class DTO {
    val q: Int = 0
    operator fun get(prop: <!UNRESOLVED_REFERENCE!>KProperty1<!><*, Int>): Int = 0
}

fun foo(intDTO: DTO?, p: <!UNRESOLVED_REFERENCE!>KProperty1<!><*, Int>) {
    if (intDTO != null) {
        intDTO[DTO::q]
        intDTO.q
    }
}

// ISSUE: KT-62137

class PersonDto (
    val name: String
)

fun test() {
    val name: String? = null
    val person: PersonDto? = null

    if (!name.isNullOrEmpty()) {
        <!DEBUG_INFO_SMARTCAST!>name<!>.length // Smart cast work
    }

    if (!person?.name.isNullOrEmpty()) {
        person<!UNSAFE_CALL!>.<!>name // Smart cast doesn't work
    }
}

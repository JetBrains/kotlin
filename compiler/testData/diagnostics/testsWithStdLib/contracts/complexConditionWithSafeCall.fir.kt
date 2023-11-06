// ISSUE: KT-62137

class PersonDto (
    val name: String
)

fun test() {
    val name: String? = null
    val person: PersonDto? = null

    if (!name.isNullOrEmpty()) {
        name.length // Smart cast work
    }

    if (!person?.name.isNullOrEmpty()) {
        person.name // Smart cast doesn't work
    }
}

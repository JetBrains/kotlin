// RUN_PIPELINE_TILL: CODEGEN
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

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, localProperty, nullableType,
primaryConstructor, propertyDeclaration, safeCall, smartcast */

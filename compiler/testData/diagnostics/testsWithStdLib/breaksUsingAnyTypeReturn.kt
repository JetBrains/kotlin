// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS

val accessMap = mutableMapOf<String, String>()

// Taken from arrow-kt
fun test(): Boolean {
    val whatever = mutableSetOf<String>()
    accessMap.forEach { tvToEntry ->
        val (tv, entry) = tvToEntry
        if (tv == entry) {
            if (tv == "this") {
                whatever += entry
            } else {
                whatever += entry
                <!ANY_TYPE_RETURN_AS_BREAK_IN_STDLIB_FUNCTION("forEach")!>return<!UNRESOLVED_LABEL!>@arrowTest<!> false<!>
            }
        } else {
            if (tv == "that") {
                <!ANY_TYPE_RETURN_AS_BREAK_IN_STDLIB_FUNCTION("forEach")!>return<!UNRESOLVED_LABEL!>@arrowTest<!> false<!>
            } else {
                whatever += entry
            }
        }
    }
    return false
}

/* GENERATED_FIR_TAGS: destructuringDeclaration, equalityExpression, functionDeclaration, ifExpression, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral */

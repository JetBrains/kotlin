// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun foo(l: List<String>, mm: MutableMap<String, String>): Iterable<String> {
    return l.flatMap { entry ->
        val childIndex = entry.hashCode()
        if (childIndex == -1) {
            // NB: Return of `foo`
            return emptyList()
        }

        // NB: Return of `foo`
        return listOf("")
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral */

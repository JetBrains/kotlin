// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun foo(l: List<String>, mm: MutableMap<String, String>): Iterable<String> {
    return l.<!OVERLOAD_RESOLUTION_AMBIGUITY!>flatMap<!> { entry ->
        val childIndex = entry.hashCode()
        if (childIndex == -1) {
            // NB: Return of `foo`
            <!RETURN_NOT_ALLOWED!>return<!> emptyList()
        }

        // NB: Return of `foo`
        <!RETURN_NOT_ALLOWED!>return<!> listOf("")
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral */

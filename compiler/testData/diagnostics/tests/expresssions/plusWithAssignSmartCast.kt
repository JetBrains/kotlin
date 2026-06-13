// RUN_PIPELINE_TILL: BACKEND
class Foo {
    var bar: String? = null

    fun addToBar(other: String) {
        if (bar == null) {
            bar = other
        } else {
            bar += " $other"
        }
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, equalityExpression, functionDeclaration,
ifExpression, nullableType, propertyDeclaration, smartcast, stringLiteral */

// RUN_PIPELINE_TILL: BACKEND
class My {
    val x: Int
    init {
        var y: Int? = null
        if (y != null) {
            x = y.hashCode()
        }
        else {
            x = 0
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, equalityExpression, ifExpression, init, integerLiteral,
localProperty, nullableType, propertyDeclaration, smartcast */

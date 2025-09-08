// RUN_PIPELINE_TILL: BACKEND
class My {
    init {
        var y: Int?
        y = 42
        <!DEBUG_INFO_SMARTCAST!>y<!>.hashCode()
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, integerLiteral, localProperty, nullableType,
propertyDeclaration, smartcast */

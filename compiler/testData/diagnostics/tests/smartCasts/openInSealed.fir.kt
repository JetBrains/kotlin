// RUN_PIPELINE_TILL: FRONTEND
sealed class My(open val x: Int?) {
    init {
        if (x != null) {
            // Should be error: property is open
            <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, ifExpression, init, nullableType, primaryConstructor,
propertyDeclaration, sealed, smartcast */

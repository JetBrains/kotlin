// RUN_PIPELINE_TILL: BACKEND
class My {
    // No initialization needed because no backing field
    val two: Int
        get() {
            val <!NAME_SHADOWING!>field<!> = 2
            return field
        }
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, localProperty, propertyDeclaration */
